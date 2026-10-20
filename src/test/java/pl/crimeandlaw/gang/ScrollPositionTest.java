package pl.crimeandlaw.gang;

import org.junit.jupiter.api.Test;
import pl.crimeandlaw.client.ScrollPosition;
import static org.junit.jupiter.api.Assertions.*;

class ScrollPositionTest {
    @Test void wheelAndThumbReachTheSameEndsWithoutOverscroll(){
        var s=new ScrollPosition();s.range(200,800);s.scroll(10000);assertEquals(600,s.offset());
        assertEquals(200-s.thumbHeight(),s.thumbTop());
        s.moveThumb(-100);assertEquals(0,s.offset());s.moveThumb(10000);assertEquals(600,s.offset());
        s.scroll(-10000);assertEquals(0,s.offset());
    }
    @Test void shortOrEmptyPageHasNoScrollingAndFullHeightThumb(){
        var s=new ScrollPosition();s.range(200,600);s.scroll(50);s.range(200,100);
        assertEquals(0,s.maximum());assertEquals(0,s.offset());assertEquals(200,s.thumbHeight());
        s.range(200,0);s.moveThumb(100);assertEquals(0,s.offset());
    }
    @Test void pageResetAndResizeKeepPositionValid(){
        var s=new ScrollPosition();s.range(200,600);s.scroll(180);s.range(300,600);assertEquals(180,s.offset());
        s.range(500,600);assertEquals(100,s.offset());s.reset();assertEquals(0,s.offset());
    }
    @Test void fractionalWheelMovementIsPreservedAndSmallViewportDoesNotDivideByZero(){
        var s=new ScrollPosition();s.range(200,400);s.scroll(.25);s.scroll(.25);assertEquals(.5,s.offset());
        s.moveThumb((200-s.thumbHeight())/2.0);assertEquals(100,s.offset());
        s.range(0,10);s.moveThumb(10);assertTrue(Double.isFinite(s.offset()));
    }
}
