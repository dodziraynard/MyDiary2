package com.idea.mydiary;

import com.idea.mydiary.models.Note;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

/**
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(JUnit4.class)
public class NoteTest {
    @Test
    public void testGetFullDate() {
        Note note = new Note("Test", 1637291715213L, "Note content");
        assertEquals(note.getFullDate(), "Fri, 19 Nov 2021");
    }

    @Test
    public void testGetDay() {
        Note note = new Note("Test", 1637291715213L, "Note content");
        assertEquals(note.getDay(), "19");
    }

    @Test
    public void testGetMonth() {
        Note note = new Note("Test", 1637291715213L, "Note content");
        assertEquals(note.getMonth(), "Nov");
    }

    @Test
    public void testGetYear() {
        Note note = new Note("Test", 1637291715213L, "Note content");
        assertEquals(note.getYear(), "2021");
    }
}