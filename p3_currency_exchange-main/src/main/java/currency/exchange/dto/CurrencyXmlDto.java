package currency.exchange.dto;

import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "ValCurs")
@Setter
public final class CurrencyXmlDto {

    private List<CurrencyDetailsXmlDto> valutes;

    public CurrencyXmlDto() {
    }

    @XmlElement(name = "Valute")
    public List<CurrencyDetailsXmlDto> getValutes() {
        return valutes;
    }
}
