package com.elibrary.infrastructure.persistence.jpa;

import com.elibrary.domain.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookJpaRepository extends JpaRepository<Book, String> {

    /**
     * Conditional update instead of read-modify-write: the database decides whether a license is
     * still free, so two concurrent borrowers can never both succeed on the last license.
     */
    @Modifying
    @Query(value = "UPDATE books SET available_licenses = available_licenses - 1, updated_at = :updatedAt "
            + "WHERE id = :id AND available_licenses > 0", nativeQuery = true)
    int acquireLicense(@Param("id") String id, @Param("updatedAt") String updatedAt);

    @Modifying
    @Query(value = "UPDATE books SET available_licenses = available_licenses + 1, updated_at = :updatedAt "
            + "WHERE id = :id AND available_licenses < total_licenses", nativeQuery = true)
    int releaseLicense(@Param("id") String id, @Param("updatedAt") String updatedAt);

    @Query("""
            select b from Book b
            where (:q is null
                   or lower(b.title) like lower(concat('%', :q, '%'))
                   or lower(b.author) like lower(concat('%', :q, '%'))
                   or lower(b.isbn) = lower(:q))
              and (:category is null or lower(b.category) = lower(:category))
              and (:availableOnly = false or b.availableLicenses > 0)
            """)
    Page<Book> search(@Param("q") String q, @Param("category") String category,
                      @Param("availableOnly") boolean availableOnly, Pageable pageable);
}
