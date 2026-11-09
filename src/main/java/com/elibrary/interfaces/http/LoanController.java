package com.elibrary.interfaces.http;

import com.elibrary.application.usecase.BorrowBookUseCase;
import com.elibrary.application.usecase.ListMyLoansUseCase;
import com.elibrary.application.usecase.ReturnLoanUseCase;
import com.elibrary.application.view.LoanView;
import com.elibrary.domain.repository.LoanStatusFilter;
import com.elibrary.domain.shared.PageQuery;
import com.elibrary.infrastructure.auth.MemberPrincipal;
import com.elibrary.interfaces.http.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping(path = "/api/v1")
@Validated
@Tag(name = "Loans", description = "Borrow, return and inspect loans")
@SecurityRequirement(name = "bearerAuth")
public class LoanController {

    private final BorrowBookUseCase borrowBook;
    private final ReturnLoanUseCase returnLoan;
    private final ListMyLoansUseCase listMyLoans;

    public LoanController(BorrowBookUseCase borrowBook, ReturnLoanUseCase returnLoan,
                          ListMyLoansUseCase listMyLoans) {
        this.borrowBook = borrowBook;
        this.returnLoan = returnLoan;
        this.listMyLoans = listMyLoans;
    }

    @PostMapping(path = "/books/{bookId}/loans", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Borrow a book",
            description = "Takes one license from the pool. Fails with 409 BOOK_UNAVAILABLE when every "
                    + "license is on loan or held for a waiting member.")
    public ResponseEntity<LoanView> borrow(@CurrentMember MemberPrincipal member,
                                           @PathVariable String bookId) {
        LoanView loan = borrowBook.execute(member.id(), bookId);
        return ResponseEntity.created(URI.create("/api/v1/loans/" + loan.id())).body(loan);
    }

    @PostMapping(path = "/loans/{loanId}/return", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Return a book",
            description = "Idempotent: returning an already returned loan responds 200 with the original "
                    + "return timestamp. The freed license is offered to the next waiting member.")
    public LoanView returnLoan(@CurrentMember MemberPrincipal member, @PathVariable String loanId) {
        return returnLoan.execute(member.id(), loanId);
    }

    @GetMapping(path = "/me/loans", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List my loans",
            description = "status accepts all (default), active, overdue or returned.")
    public PageResponse<LoanView> myLoans(
            @CurrentMember MemberPrincipal member,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(listMyLoans.execute(member.id(), LoanStatusFilter.from(status),
                new PageQuery(page, size)));
    }
}
