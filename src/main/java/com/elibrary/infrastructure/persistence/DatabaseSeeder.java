package com.elibrary.infrastructure.persistence;

import com.elibrary.domain.model.Book;
import com.elibrary.domain.model.Member;
import com.elibrary.domain.model.MembershipType;
import com.elibrary.domain.repository.BookRepository;
import com.elibrary.domain.repository.MemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Idempotent demo data so the service is usable immediately after startup. Production deployments
 * would replace this with a catalogue import and real identity management.
 */
@Component
public class DatabaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final BookRepository books;
    private final MemberRepository members;
    private final Clock clock;
    private final boolean enabled;

    public DatabaseSeeder(BookRepository books, MemberRepository members, Clock clock,
                          @Value("${elibrary.seed.enabled:true}") boolean enabled) {
        this.books = books;
        this.members = members;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || members.count() > 0) {
            return;
        }
        Instant now = clock.instant();

        members.save(Member.create("member-alice", "Alice Chen", "alice@example.com",
                MembershipType.PREMIUM, 5, "token-alice", now));
        members.save(Member.create("member-bob", "Bob Lin", "bob@example.com",
                MembershipType.BASIC, 3, "token-bob", now));
        members.save(Member.create("member-carol", "Carol Wu", "carol@example.com",
                MembershipType.BASIC, 1, "token-carol", now));

        seedBooks(now).forEach(books::save);
        log.info("Seeded {} books and 3 demo members (tokens: token-alice, token-bob, token-carol)",
                books.count());
    }

    private List<Book> seedBooks(Instant now) {
        return List.of(
                book("Clean Code", "Robert C. Martin", "9780132350884", "Software Engineering",
                        "Prentice Hall", 2008, 2, 14, now),
                book("Designing Data-Intensive Applications", "Martin Kleppmann", "9781449373320",
                        "Software Engineering", "O'Reilly Media", 2017, 3, 14, now),
                book("Domain-Driven Design", "Eric Evans", "9780321125217", "Software Architecture",
                        "Addison-Wesley", 2003, 1, 14, now),
                book("The Pragmatic Programmer", "David Thomas, Andrew Hunt", "9780135957059",
                        "Software Engineering", "Addison-Wesley", 2019, 2, 14, now),
                book("Refactoring", "Martin Fowler", "9780134757599", "Software Engineering",
                        "Addison-Wesley", 2018, 1, 14, now),
                book("Introduction to Algorithms", "Thomas H. Cormen", "9780262046305",
                        "Computer Science", "MIT Press", 2022, 2, 21, now),
                book("The Art of Computer Programming", "Donald E. Knuth", "9780201896831",
                        "Computer Science", "Addison-Wesley", 1997, 1, 21, now),
                book("Structure and Interpretation of Computer Programs", "Harold Abelson",
                        "9780262510875", "Computer Science", "MIT Press", 1996, 1, 21, now),
                book("Working Effectively with Legacy Code", "Michael C. Feathers", "9780131177055",
                        "Software Engineering", "Prentice Hall", 2004, 2, 14, now),
                book("Release It!", "Michael T. Nygard", "9781680502398", "Software Architecture",
                        "Pragmatic Bookshelf", 2018, 1, 14, now),
                book("Accelerate", "Nicole Forsgren", "9781942788331", "Engineering Management",
                        "IT Revolution Press", 2018, 2, 14, now),
                book("A Philosophy of Software Design", "John Ousterhout", "9781732102200",
                        "Software Architecture", "Yaknyam Press", 2021, 1, 14, now));
    }

    private Book book(String title, String author, String isbn, String category, String publisher,
                      int year, int licenses, int loanPeriodDays, Instant now) {
        return Book.create(UUID.randomUUID().toString(), title, author, isbn, category, publisher,
                year, "en", "Demo catalogue entry for " + title + ".", null, licenses, loanPeriodDays, now);
    }
}
