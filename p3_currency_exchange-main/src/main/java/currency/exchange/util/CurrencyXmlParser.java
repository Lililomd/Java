package currency.exchange.util;

import currency.exchange.dto.CurrencyDetailsXmlDto;
import currency.exchange.dto.CurrencyXmlDto;
import currency.exchange.repository.CurrencyRepository;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.List;

public final class CurrencyXmlParser {

    @Value("${update.urlUpdate}")
    private String url;
    @Autowired
    private CurrencyRepository currencyRepository;
    @Autowired
    private UtilDataBase utilDataBase;

    @PostConstruct
    @Scheduled(fixedRateString = "${update.timeUpdate}")
    public void xmlClient() {
        HttpClientXml httpClientXml = new HttpClientXml();
        String xml = httpClientXml.getCurrencies(url);
        CurrencyXmlDto currencyXmlDto = toObject(xml);
        List<CurrencyDetailsXmlDto> currencyDetailsXmlDtos = currencyXmlDto.getValutes();
        for (CurrencyDetailsXmlDto currencyDetails : currencyDetailsXmlDtos) {
            Double value = Double.valueOf(currencyDetails.getValue().replace(",", "."));
            if(!currencyRepository.existsByName(currencyDetails.getName())) {
                utilDataBase.createCurrency(currencyDetails, value);
            } else {
                utilDataBase.updateCurrency(currencyDetails,value);
            }
        }
    }

    @SneakyThrows
    public static CurrencyXmlDto toObject(String xml) {
        JAXBContext jaxbContext = JAXBContext.newInstance(CurrencyXmlDto.class);
        Unmarshaller jaxUnmarshaller = jaxbContext.createUnmarshaller();
        return (CurrencyXmlDto) jaxUnmarshaller.unmarshal(new StringReader(xml));
    }
}
