package currency.exchange.util;

import currency.exchange.dto.CurrencyDetailsXmlDto;
import currency.exchange.entity.Currency;
import currency.exchange.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UtilDataBase {

    @Autowired
    private CurrencyRepository currencyRepository;

    public void createCurrency(CurrencyDetailsXmlDto currencyDetails, Double value) {
        Currency currency = new Currency();
        currency.setCharCode(currencyDetails.getCharCode());
        currency.setName(currencyDetails.getName());
        currency.setValue(value);
        currency.setNominal(currencyDetails.getNominal());
        currency.setIsoNumCode(currencyDetails.getIsoNumCode());
        currencyRepository.saveAndFlush(currency);
    }

    public void updateCurrency(CurrencyDetailsXmlDto currencyDetails, Double value) {
        Currency currency = currencyRepository.findByIsoNumCode(currencyDetails.getIsoNumCode());
        currency.setValue(value);
        currency.setCharCode(currencyDetails.getCharCode());
        currencyRepository.saveAndFlush(currency);
    }
}
