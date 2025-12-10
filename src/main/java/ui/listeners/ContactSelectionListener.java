package ui.listeners;

import people.Person;

public interface ContactSelectionListener {
    void onContactSelected(Person person);
    void onDeleteContact(Person person);
}