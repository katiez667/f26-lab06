package edu.cmu.cs214.booking;

/**
 * Everything needed to create one booking, passed to
 * {@link BookingApi#createBooking(BookingRequest)}.
 *
 * <p>Start with {@link #of(String, long, long)} and add the optional fields
 * with {@link #withWaitlistKey(String)} and {@link #withNotes(String)}. Each
 * {@code with} method returns a new request; requests are immutable.
 *
 * <p>The request does not validate itself. {@code createBooking} checks it and
 * throws {@link IllegalArgumentException} as its javadoc describes.
 *
 * @param roomId      the room to book, non-null
 * @param startMinute first minute of the booking, inclusive
 * @param endMinute   first minute after the booking, exclusive; must be
 *                    greater than {@code startMinute}
 * @param waitlistKey caller's waitlist key, or null to decline waitlisting
 * @param notes       free-text notes for the booking, or null for none
 */
public record BookingRequest(String roomId, long startMinute, long endMinute,
                             String waitlistKey, String notes) {

    /** A request for the range with no waitlist key and no notes. */
    public static BookingRequest of(String roomId, long startMinute, long endMinute) {
        return new BookingRequest(roomId, startMinute, endMinute, null, null);
    }

    /** This request, but waitlisting on conflict under {@code key}. */
    public BookingRequest withWaitlistKey(String key) {
        return new BookingRequest(roomId, startMinute, endMinute, key, notes);
    }

    /** This request, but carrying {@code text} as its notes. */
    public BookingRequest withNotes(String text) {
        return new BookingRequest(roomId, startMinute, endMinute, waitlistKey, text);
    }
}
