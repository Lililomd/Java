package currency.exchange.dto;

import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;

@Setter
public final class CurrencyDetailsXmlDto {
    private String name;
    private Long nominal;
    private String value;
    private Long isoNumCode;
    private String charCode;

    @XmlElement(name = "Name")
    public String getName() {
        return name;
    }

    @XmlElement(name = "Nominal")
    public Long getNominal() {
        return nominal;
    }

    @XmlElement(name = "Value")
    public String getValue() {
        return value;
    }

    @XmlElement(name = "NumCode")
    public Long getIsoNumCode() {
        return isoNumCode;
    }

    @XmlElement(name = "CharCode")
    public String getCharCode() {
        return charCode;
    }
}
