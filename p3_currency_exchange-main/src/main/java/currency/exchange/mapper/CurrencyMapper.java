package currency.exchange.mapper;

import currency.exchange.dto.CurrencyDto;
import currency.exchange.entity.Currency;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CurrencyMapper {

    CurrencyDto convertToDto(Currency currency);

    Currency convertToEntity(CurrencyDto currencyDto);
}
