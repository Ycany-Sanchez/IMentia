package util;

import org.junit.jupiter.api.Test;

import people.Person;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileHandlerTest {

    @Test
    void capitalizeLabel_capitalizesEachWord() {
        assertThat(FileHandler.capitalizeLabel("maria santos")).isEqualTo("Maria Santos");
        assertThat(FileHandler.capitalizeLabel("JUAN DELA CRUZ")).isEqualTo("Juan Dela Cruz");
        assertThat(FileHandler.capitalizeLabel("ana")).isEqualTo("Ana");
    }

    @Test
    void capitalizeLabel_handlesNullAndEmpty() {
        assertThat(FileHandler.capitalizeLabel(null)).isNull();
        assertThat(FileHandler.capitalizeLabel("")).isEqualTo("");
    }

    @Test
    void generateId_emptyListStartsAtOne() {
        assertThat(FileHandler.generateId(new ArrayList<>())).isEqualTo("Person1");
    }

    @Test
    void generateId_returnsMaxPlusOne() {
        List<Person> persons = new ArrayList<>();
        persons.add(personWithId("Person1"));
        persons.add(personWithId("Person2"));
        assertThat(FileHandler.generateId(persons)).isEqualTo("Person3");
    }

    @Test
    void generateId_handlesGapsAndUnsortedOrder() {
        List<Person> persons = new ArrayList<>();
        persons.add(personWithId("Person5"));
        persons.add(personWithId("Person2"));
        assertThat(FileHandler.generateId(persons)).isEqualTo("Person6");
    }

    @Test
    void generateId_ignoresNullAndMalformedIds() {
        List<Person> persons = new ArrayList<>();
        persons.add(personWithId("Person3"));
        persons.add(personWithId(null));
        persons.add(personWithId("PersonTest-ab12cd34"));
        persons.add(personWithId("bogus"));
        persons.add(null);
        assertThat(FileHandler.generateId(persons)).isEqualTo("Person4");
    }

    @Test
    void csvHelpers_quoteAndParseRoundTrip() {
        assertThat(FileHandler.escapeCsv("plain")).isEqualTo("plain");
        assertThat(FileHandler.escapeCsv("Dela Cruz, Jr.")).isEqualTo("\"Dela Cruz, Jr.\"");
        assertThat(FileHandler.parseCsvLine("Person1,\"Dela Cruz, Jr.\",Son"))
                .containsExactly("Person1", "Dela Cruz, Jr.", "Son");
        assertThat(FileHandler.parseCsvLine("Person1,Ada,Mother"))
                .containsExactly("Person1", "Ada", "Mother");
    }

    private static Person personWithId(String id) {
        Person p = new Person("Test", "Friend");
        p.setId(id);
        return p;
    }
}
