package com.after_sunrise.cryptocurrency.cryptotrader.service.bitbank;

import cc.bitbank.Bitbankcc;
import cc.bitbank.entity.Assets;
import cc.bitbank.entity.Depth;
import cc.bitbank.entity.Transactions;
import com.after_sunrise.cryptocurrency.cryptotrader.framework.Trade;
import com.after_sunrise.cryptocurrency.cryptotrader.service.template.TemplateContext;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static java.math.BigDecimal.ONE;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

/**
 * @author takanori.takase
 * @version 0.0.1
 */
public class BitbankContext extends TemplateContext implements BitbankService {

    private final ThreadLocal<Bitbankcc> localApi;

    protected BitbankContext() {

        super(ID);

        localApi = ThreadLocal.withInitial(Bitbankcc::new);

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

            return Collections.unmodifiableList(Stream.of(t.transactions)
                    .filter(Objects::nonNull).map(BitbankTransaction::new).collect(toList()));

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
                .filter(Objects::nonNull).collect(toList());
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

        String id = StringUtils.trimToEmpty(currency.name());

        Key k = Key.build(key).instrument(WILDCARD).build();

        List<BitbankAsset> assets = trimToEmpty(listCached(BitbankAsset.class, k, () -> {

            Assets a = getLocalApi().getAsset();

            if (a == null || ArrayUtils.isEmpty(a.assets)) {
                return emptyList();
            }

            return Stream.of(a.assets).filter(Objects::nonNull)
                    .map(BitbankAsset::new).collect(toList());

        }));

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

}
