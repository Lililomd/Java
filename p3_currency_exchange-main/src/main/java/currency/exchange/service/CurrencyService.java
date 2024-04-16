package currency.exchange.service;

import currency.exchange.dto.CurrencyAllDto;
import currency.exchange.dto.CurrencyDto;
import currency.exchange.entity.Currency;
import currency.exchange.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import currency.exchange.mapper.CurrencyMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {
    private final CurrencyMapper mapper;
    private final CurrencyRepository repository;

    public List<CurrencyAllDto> getAll() {
        List<Currency> currencies = repository.findAll();
        List<CurrencyAllDto> currencyAllDtos = new ArrayList<>();
        for(Currency currency : currencies) {
            CurrencyAllDto currencyAllDto =  new CurrencyAllDto();
            currencyAllDto.setName(currency.getName());
            currencyAllDto.setValue(currency.getValue());
            currencyAllDtos.add(currencyAllDto);
        }
        return currencyAllDtos;
    }
    public CurrencyDto getById(Long id) {
        log.info("CurrencyService method getById executed");
        Currency currency = repository.findById(id).orElseThrow(() -> new RuntimeException("Currency not found with id: " + id));
        return mapper.convertToDto(currency);
    }

    public Double convertValue(Long value, Long numCode) {
        log.info("CurrencyService method convertValue executed");
        Currency currency = repository.findByIsoNumCode(numCode);
        return value * currency.getValue();
    }

    public CurrencyDto create(CurrencyDto dto) {
        log.info("CurrencyService method create executed");
        return  mapper.convertToDto(repository.save(mapper.convertToEntity(dto)));
    }
}
