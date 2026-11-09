package com.elibrary.interfaces.http;

import com.elibrary.application.usecase.BrowseBooksUseCase;
import com.elibrary.application.usecase.GetBookDetailUseCase;
import com.elibrary.application.view.BookDetailView;
import com.elibrary.application.view.BookSummaryView;
import com.elibrary.domain.repository.BookSearchCriteria;
import com.elibrary.domain.repository.BookSort;
import com.elibrary.domain.shared.PageQuery;
import com.elibrary.infrastructure.auth.MemberPrincipal;
import com.elibrary.interfaces.http.security.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping(path = "/api/v1/books", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Tag(name = "Books", description = "Browse and inspect the digital catalogue")
public class BookController {

    private final BrowseBooksUseCase browseBooks;
    private final GetBookDetailUseCase getBookDetail;

    public BookController(BrowseBooksUseCase browseBooks, GetBookDetailUseCase getBookDetail) {
        this.browseBooks = browseBooks;
        this.getBookDetail = getBookDetail;
    }

    @GetMapping
    @Operation(summary = "Browse or search the catalogue",
            description = "Public endpoint. Supports free-text search over title/author/ISBN, category "
                    + "filter, availability filter, sorting and zero-based pagination.")
    public PageResponse<BookSummaryView> browse(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean availableOnly,
            @RequestParam(defaultValue = "title") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(PageQuery.MAX_SIZE) int size) {
        return PageResponse.from(browseBooks.execute(
                new BookSearchCriteria(q, category, availableOnly),
                BookSort.from(sort),
                new PageQuery(page, size)));
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "Get book details",
            description = "Public endpoint. When a bearer token is supplied, borrowedByMe reflects the caller.")
    public BookDetailView detail(@PathVariable String bookId,
                                 @CurrentMember(required = false) MemberPrincipal member) {
        return getBookDetail.execute(bookId, Optional.ofNullable(member).map(MemberPrincipal::id));
    }
}
