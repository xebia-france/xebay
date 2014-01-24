package fr.xebia.xebay.domain;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ItemsTest {
    @Test
    public void load_one_item() {
        Items items = Items.load("item").get();

        Item item = items.next();
        assertThat(item.getName()).isEqualTo("item");
        assertThat(item.getValue()).isEqualTo(1d);
    }

    @Test
    public void load_two_items() {
        Items items = Items.load("items").get();
        items.next();

        Item secondItem = items.next();
        assertThat(secondItem.getName()).isEqualTo("second item");
        assertThat(secondItem.getValue()).isEqualTo(2d);
    }

    @Test
    public void load_item_with_accented_chars() {
        Items items = Items.load("items-accented-chars").get();

        Item item = items.next();
        assertThat(item.getName()).isEqualTo("itèm with accented character");
    }

    @Test
    public void load_item_with_comma() {
        Items items = Items.load("items-comma").get();

        Item item = items.next();
        assertThat(item.getName()).isEqualTo("item with a , comma");
    }

    @Test
    public void load_item_with_trimmed_columns() {
        Items items = Items.load("items-trim").get();

        Item item = items.next();
        assertThat(item.getName()).isEqualTo("item");
        assertThat(item.getValue()).isEqualTo(1d);
    }

    @Test(expected = BidException.class)
    public void can_t_create_items_without_any_item() {
        new Items();
    }

    @Test
    public void should_returns_first_item() {
        Items items = new Items(new Item("an item", 4.3));

        Item item = items.next();

        assertThat(item.getName()).isEqualTo("an item");
    }

    @Test
    public void should_returns_second_item() {
        Items items = new Items(new Item("an item", 4.3), new Item("another item", 2.4));
        items.next();

        Item item = items.next();

        assertThat(item.getName()).isEqualTo("another item");
    }

    @Test
    public void should_loop_througth_items() {
        Items items = new Items(new Item("an item", 4.3), new Item("another item", 2.4));
        items.next();
        items.next();

        Item item = items.next();

        assertThat(item.getName()).isEqualTo("an item");
    }
}
