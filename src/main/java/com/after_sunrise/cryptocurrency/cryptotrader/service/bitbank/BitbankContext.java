package com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank;

import cc.bitbank.Bitbankcc;
import cc.bitbank.entity.Assets;
import cc.bitbank.entity.Depth;
import cc.bitbank.entity.Orders;
import cc.bitbank.entity.Transactions;
import cc.bitbank.entity.enums.CurrencyPair;
import cc.bitbank.entity.enums.OrderSide;
import cc.bitbank.entity.enums.OrderType;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CancelInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Instruction.CreateInstruction;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Order.Execution;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank.BitbankOrder.BitbankExecution;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.lang.Boolean.FALSE;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitbankContext extends TemplateContext implements BitbankService {

    private static final Pattern NUMERIC = Pattern.compile("^[0-9]+$");

    private final ThreadLocal<Bitbankcc> localApi;

    private final NavigableMap<Long, BitbankOrder> cachedOrders;

    public BitbankContext() {

        super(ID);

        localApi = ThreadLocal.withInitial(Bitbankcc::new);

        cachedOrders = new ConcurrentSkipListMap<>();

    }

    @VisibleForTesting
    NavigableMap<Long, BitbankOrder> getCachedOrders() {
        return cachedOrders;
    }

    @VisibleForTesting
    Bitbankcc getLocalApi() {

        String apiKey = getStringProperty("api.id", null);

        String secret = getStringProperty("api.secret", null);

        return localApi.get().setKey(apiKey, secret);

    }

    @VisibleForTesting
    Optional<BitbankDepth> fetchDepth(Key key) {

        return Optional.ofNullable(findCached(BitbankDepth.class, key, () -> {

            ProductType product = ProductType.find(key.getInstrument());

            if (product == null || product.getPair() == null) {
                return null;
            }

            Depth depth = getLocalApi().getDepth(product.getPair());

            if (depth == null) {
                return null;
            }

            return new BitbankDepth(depth);

        }));

    }

    @Override
    public BigDecimal getBestAskPrice(Key key) {
        return fetchDepth(key).map(BitbankDepth::getAsks)
                .map(NavigableMap::firstEntry)
                .map(Map.Entry::getKey).orElse(null);
    }

    @Override
    public BigDecimal getBestBidPrice(Key key) {
        return fetchDepth(key).map(BitbankDepth::getBids)
                .map(NavigableMap::firstEntry)
                .map(Map.Entry::getKey).orElse(null);
    }

    @Override
    public BigDecimal getBestAskSize(Key key) {
        return fetchDepth(key).map(BitbankDepth::getAsks)
                .map(NavigableMap::firstEntry)
                .map(Map.Entry::getValue).orElse(null);
    }

    @Override
    public BigDecimal getBestBidSize(Key key) {
        return fetchDepth(key).map(BitbankDepth::getBids)
                .map(NavigableMap::firstEntry)
                .map(Map.Entry::getValue).orElse(null);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getAskPrices(Key key) {
        return fetchDepth(key).map(BitbankDepth::getAsks).orElse(null);
    }

    @Override
    public Map<BigDecimal, BigDecimal> getBidPrices(Key key) {
        return fetchDepth(key).map(BitbankDepth::getBids).orElse(null);
    }

    @VisibleForTesting
    List<BitbankTransaction> fetchTransactions(Key key) {

        return listCached(BitbankTransaction.class, key, () -> {

            ProductType product = ProductType.find(key.getInstrument());

            if (product == null) {
                return emptyList();
            }

            Transactions t = getLocalApi().getTransaction(product.getPair());

            if (t == null || ArrayUtils.isEmpty(t.transactions)) {
                return emptyList();
            }

            return unmodifiableList(Stream.of(t.transactions)
                    .filter(Objects::nonNull)
                    .map(BitbankTransaction::new)
                    .collect(toList())
            );

        });

    }

    @Override
    public BigDecimal getLastPrice(Key key) {
        return trimToEmpty(fetchTransactions(key)).stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getTimestamp() != null)
                .max(Comparator.comparing(BitbankTransaction::getTimestamp))
                .map(BitbankTransaction::getPrice)
                .orElse(null);
    }

    @Override
    public List<Trade> listTrades(Key key, Instant fromTime) {
        return trimToEmpty(fetchTransactions(key)).stream()
                .filter(Objects::nonNull)
                .filter(t -> t.getTimestamp() != null)
                .filter(t -> fromTime == null || !t.getTimestamp().isBefore(fromTime))
                .collect(toList());
    }

    @Override
    public CurrencyType getInstrumentCurrency(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        return product == null ? null : product.getInstrumentCurrency();

    }

    @Override
    public CurrencyType getFundingCurrency(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        return product == null ? null : product.getFundingCurrency();

    }

    @Override
    public String findProduct(Key key, CurrencyType instrument, CurrencyType funding) {

        for (ProductType product : ProductType.values()) {

            if (instrument != product.getInstrumentCurrency()) {
                continue;
            }

            if (funding != product.getFundingCurrency()) {
                continue;
            }

            return product.name();

        }

        return null;

    }

    @Override
    public BigDecimal getConversionPrice(Key key, CurrencyType currency) {

        if (currency == null) {
            return null;
        }

        if (currency == getInstrumentCurrency(key)) {
            return ONE;
        }

        if (currency == getFundingCurrency(key)) {

            BigDecimal p = getMidPrice(key);

            return p == null ? null : p.negate();

        }

        return null;

    }

    @VisibleForTesting
    BigDecimal fetchBalance(Key key, CurrencyType currency) {

        if (currency == null) {
            return null;
        }

        Key k = Key.build(key).instrument(WILDCARD).build();

        List<BitbankAsset> assets = trimToEmpty(listCached(BitbankAsset.class, k, () -> {

            Assets a = getLocalApi().getAsset();

            if (a == null || ArrayUtils.isEmpty(a.assets)) {
                return emptyList();
            }

            return Stream.of(a.assets).filter(Objects::nonNull)
                    .map(BitbankAsset::new).collect(toList());

        }));

        String id = StringUtils.trimToEmpty(currency.name());

        return trimToEmpty(assets).stream()
                .filter(Objects::nonNull)
                .filter(a -> id.equalsIgnoreCase(a.getId()))
                .findFirst().map(BitbankAsset::getBalance).orElse(null);

    }

    @Override
    public BigDecimal getInstrumentPosition(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        return product == null ? null : fetchBalance(key, product.getInstrumentCurrency());

    }

    @Override
    public BigDecimal getFundingPosition(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        return product == null ? null : fetchBalance(key, product.getFundingCurrency());

    }

    @Override
    public BigDecimal roundLotSize(Key key, BigDecimal value, RoundingMode mode) {

        ProductType product = ProductType.find(key.getInstrument());

        return product == null ? null : super.round(value, mode, product.getLotSize());

    }

    @Override
    public BigDecimal roundTickSize(Key key, BigDecimal value, RoundingMode mode) {

        ProductType product = ProductType.find(key.getInstrument());

        return product == null ? null : super.round(value, mode, product.getTickSize());

    }

    @Override
    public BigDecimal getCommissionRate(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        return getDecimalProperty("commission." + product.name(), ZERO);

    }

    @Override
    public Boolean isMarginable(Key key) {
        return FALSE;
    }

    @Override
    public ZonedDateTime getExpiry(Key key) {
        return null;
    }

    @Override
    public BitbankOrder findOrder(Key key, String id) {

        if (StringUtils.isEmpty(id) || !NUMERIC.matcher(id).matches()) {
            return null;
        }

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return null;
        }

        Key idKey = Key.build(key).instrument(key.getInstrument() + "@" + id).build();

        return findCached(BitbankOrder.class, idKey, () -> {

            cc.bitbank.entity.Order o = getLocalApi().getOrder(product.getPair(), Long.valueOf(id));

            return o == null ? null : new BitbankOrder(o);

        });

    }

    @VisibleForTesting
    List<BitbankOrder> fetchActiveOrders(Key key) {

        ProductType product = ProductType.find(key.getInstrument());

        if (product == null) {
            return emptyList();
        }

        List<BitbankOrder> orders = listCached(BitbankOrder.class, key, () -> {

            Map<String, Long> options = singletonMap("count", 100L);

            Orders result = getLocalApi().getActiveOrders(product.getPair(), options);

            if (result == null || ArrayUtils.isEmpty(result.orders)) {
                return emptyList();
            }

            return Collections.unmodifiableList(
                    Stream.of(result.orders).filter(Objects::nonNull).map(BitbankOrder::new).collect(toList())
            );

        });

        return orders;

    }


    @Override
    public List<Order> listActiveOrders(Key key) {

        List<BitbankOrder> orders = trimToEmpty(fetchActiveOrders(key));

        return orders.stream().filter(Objects::nonNull).collect(toList());

    }

    @Override
    public List<Execution> listExecutions(Key key) {

        // API not available : https://bitbank.cc/blog/20171227trade-history/

        List<Execution> executions = new ArrayList<>();

        for (Map.Entry<Long, BitbankOrder> entry : cachedOrders.entrySet()) {

            Long id = entry.getKey();

            BitbankOrder order = entry.getValue();

            if (order == null || !Objects.equals(FALSE, order.getActive())) {

                order = findOrder(key, id.toString());

                if (order != null) {
                    cachedOrders.put(id, order);
                }

            }

            if (order != null) {

                if (order.getFilledQuantity() != null && order.getFilledQuantity().signum() != 0) {

                    executions.add(new BitbankExecution(order.getDelegate()));

                    continue;

                }

                if (!Objects.equals(FALSE, order.getActive())) {
                    continue;
                }

            }

            cachedOrders.remove(id);

        }

        return executions;

    }

    @Override
    public Map<CreateInstruction, String> createOrders(Key key, Set<CreateInstruction> instructions) {

        Map<CreateInstruction, String> results = new IdentityHashMap<>();

        for (CreateInstruction i : trimToEmpty(instructions)) {

            if (i == null || i.getPrice() == null || i.getSize() == null || i.getSize().signum() == 0) {

                results.put(i, null);

                continue;

            }

            ProductType product = ProductType.find(key.getInstrument());

            if (product == null) {

                results.put(i, null);

                continue;

            }

            try {

                CurrencyPair pair = product.getPair();
                BigDecimal price = i.getPrice().signum() != 0 ? i.getPrice() : null;
                OrderType type = i.getPrice().signum() != 0 ? OrderType.LIMIT : OrderType.MARKET;
                BigDecimal amount = i.getSize().abs();
                OrderSide side = i.getSize().signum() >= 0 ? OrderSide.BUY : OrderSide.SELL;

                cc.bitbank.entity.Order order = getLocalApi().sendOrder(pair, price, amount, side, type);

                if (order != null && order.status != null) {

                    results.put(i, String.valueOf(order.orderId));

                    int capacity = getIntProperty("cache.order", 32);

                    while (true) {

                        if (cachedOrders.isEmpty()) {
                            break;
                        }

                        if (cachedOrders.size() < capacity) {
                            break;
                        }

                        cachedOrders.pollFirstEntry();

                    }

                    cachedOrders.put(order.orderId, new BitbankOrder(order));

                } else {

                    // Failed orders have empty fields
                    results.put(i, null);

                }


            } catch (Exception e) {

                log.warn("Order create failure : " + i, e);

                results.put(i, null);

            }

        }

        return results;

    }

    @Override
    public Map<CancelInstruction, String> cancelOrders(Key key, Set<CancelInstruction> instructions) {

        Map<CancelInstruction, String> results = new IdentityHashMap<>();

        ProductType product = ProductType.find(key.getInstrument());

        if (product != null) {

            trimToEmpty(instructions).stream()
                    .filter(Objects::nonNull)
                    .filter(i -> StringUtils.isNotBlank(i.getId()))
                    .filter(i -> NUMERIC.matcher(i.getId()).matches())
                    .forEach(i -> {

                        String result;

                        try {

                            long id = Long.valueOf(i.getId());

                            cc.bitbank.entity.Order order = getLocalApi().cancelOrder(product.getPair(), id);

                            result = order == null ? null : new BitbankOrder(order).getId();

                        } catch (Exception e) {

                            log.warn("Order cancel failure : " + i.getId(), e);

                            result = null;

                        }

                        results.put(i, result);

                    });

        }

        return results;

    }

}
