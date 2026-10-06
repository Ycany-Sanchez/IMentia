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

    private static Person personWithId(String id) {
        Person p = new Person("Test", "Friend");
        p.setId(id);
        return p;
    }
}
