package com.idea.mydiary;

import com.idea.mydiary.models.Media;
import com.idea.mydiary.models.Note;
import com.idea.mydiary.types.MediaType;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

/*
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(JUnit4.class)
public class MediaTest {
    @Test
    public void testMediaType() {
        Media media = new Media("AUDIO", "url", 2L);
        assertEquals(media.getMediaType(), MediaType.AUDIO.name());
    }
}