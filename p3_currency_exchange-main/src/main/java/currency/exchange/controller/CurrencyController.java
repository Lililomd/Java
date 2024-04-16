package currency.exchange.controller;

import currency.exchange.dto.CurrencyAllDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import currency.exchange.dto.CurrencyDto;
import currency.exchange.service.CurrencyService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/currency")
public class CurrencyController {
    private final CurrencyService service;

    @GetMapping(value = "/")
    ResponseEntity<List<CurrencyAllDto>> getAll() {
        List<CurrencyAllDto> currencyAllDtos = service.getAll();
        return ResponseEntity.ok(currencyAllDtos);
    }

    @GetMapping(value = "/{id}")
    ResponseEntity<CurrencyDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping(value = "/convert")
    ResponseEntity<Double> convertValue(@RequestParam("value") Long value, @RequestParam("numCode") Long numCode) {
        return ResponseEntity.ok(service.convertValue(value, numCode));
    }

    @PostMapping("/create")
    ResponseEntity<CurrencyDto> create(@RequestBody CurrencyDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }
}
