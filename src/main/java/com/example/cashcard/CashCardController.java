package com.example.cashcard;

import java.security.Principal;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/cashcards")
public class CashCardController {

    private final CashCardRepository cashCardRepository;

    private CashCardController(CashCardRepository cashCardRepository) {
        this.cashCardRepository = cashCardRepository;
    }

    private Optional<CashCard> findCashCard(Long id, Principal principal) {
        return Optional.ofNullable(this.cashCardRepository.findByIdAndOwner(id, principal.getName()));
    }

    @GetMapping("/{requestedId}")
    private ResponseEntity<CashCard> findById(@PathVariable("requestedId") Long requestedId, Principal principal) {
        return findCashCard(requestedId, principal)
                .map(cashCard -> ResponseEntity.ok(cashCard))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping()
    private ResponseEntity<Iterable<CashCard>> findAll(Pageable pageable, Principal principal) {
        var page = cashCardRepository.findByOwner(principal.getName(), PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by(Sort.Direction.ASC, "amount"))));

        return ResponseEntity.ok(page.getContent());
    }

    @PostMapping
    private ResponseEntity<Void> createCashCard(
            @RequestBody CashCard newCashCardRequest,
            UriComponentsBuilder ucb,
            Principal principal) {

        if (newCashCardRequest == null)
            return ResponseEntity.badRequest().build();

        var cashCardWithOwner = new CashCard(null, newCashCardRequest.amount(), principal.getName());
        var savedCashCard = cashCardRepository.save(cashCardWithOwner);
        var locationOfNewCashCard = ucb
                .path("cashcards/{id}")
                .buildAndExpand(savedCashCard.id())
                .toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }

    @PutMapping("/{requestedId}")
    private ResponseEntity<Void> putCashCard(
            @PathVariable("requestedId") Long requestedId,
            @RequestBody CashCard cashCardUpdate,
            Principal principal) {

        return findCashCard(requestedId, principal).map(cashCard -> {
            var updatedCashCard = new CashCard(cashCard.id(), cashCardUpdate.amount(), principal.getName());
            cashCardRepository.save(updatedCashCard);
            ResponseEntity<Void> response = ResponseEntity.noContent().build();
            return response;
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    private ResponseEntity<Void> deleteCashCard(@PathVariable("id") Long id, Principal principal) {

        if (id == null)
            return ResponseEntity.badRequest().build();

        return findCashCard(id, principal).map(cashCard -> {
            cashCardRepository.deleteById(id);
            ResponseEntity<Void> response = ResponseEntity.noContent().build();
            return response;
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
