# Contract Worksheet

One section per milestone. Fill each one in as you go, in order. Write each
prediction before you run anything. That is the part a TA asks about.

Keep it short and specific. Point at methods, call sites, and error text.

---

## Milestone 1: The notes overload

### Prediction (write this before you run the build, and you can deliberate with your agent)

**Will the consumer, untouched, still compile and pass?** Yes or no.

Yes.

**Why.** What does the compiler do with the consumer's existing call sites once
the new overload exists?

`FrontDesk.java:27` and `:33` pass four arguments, so the compiler still binds them to the
original 4-arg `createBooking` (the 5-arg overload isn't a candidate, even for the literal `null`),
and since that method keeps its behavior, the consumer never notices.

### What happened

**The result.** What the build printed for each module.

`mvn -B clean test`:

```
[INFO] Building lab06-api 1.0.0                                           [2/3]
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Building lab06-consumer 1.0.0                                      [3/3]
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] lab06-api .......................................... SUCCESS
[INFO] lab06-consumer ..................................... SUCCESS
[INFO] BUILD SUCCESS
```

**If your prediction was wrong,** say what you missed.

Prediction was right. The consumer recompiled against the new API with no errors or warnings,
and the old 4-arg method now delegates to the 5-arg one with `notes = null`, so behavior is identical.

**Is an additive change always safe in Java?** One case where adding something
to an API still breaks a caller, if you can name one.

No. The new overload is an abstract method on a public interface, so any class outside `api/`
that `implements BookingApi` (a test fake, a caching wrapper) would stop compiling until it
implements the 5-arg method. The consumer only survived because it uses our
`InMemoryBookingService` rather than implementing the interface itself.

---

## Milestone 2: The request object

### Prediction (write this before you run the build)

**Will the untouched consumer still compile and pass?** Yes or no, and if no,
which module goes red and whether at compile time or test time.

No. `lab06-consumer` goes red at compile time (`testCompile` never even starts),
because the 4-arg `createBooking` it calls no longer exists. Its 7 tests never run.

**Where.** Name the call sites you expect to be affected, if any.

`FrontDesk.java:27` (`bookWalkIn`: `api.createBooking(roomId, startMinute, endMinute, null)`) and
`FrontDesk.java:33` (`joinWaitlist`: `api.createBooking(roomId, startMinute, endMinute, guestName)`).
The `listBookings` and `cancelBooking` calls are untouched and should be fine.

**What about the tests in `api/`, after you update them?** And whether their
result is evidence about the consumer.

They'll pass (5/5), but that is not evidence about the consumer. I rewrote them to the new
call, so they only prove the new API works. They can't tell me whether old callers still build.
Only the consumer's own build can detect that break.

### Step 1: after the fold

**What the build printed.** Paste it for each module, including file and
line for anything that failed.

`mvn -B clean test`, **`lab06-api`**:

```
[INFO] Building lab06-api 1.0.0                                           [2/3]
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
```

**`lab06-consumer`**:

```
[INFO] Building lab06-consumer 1.0.0                                      [3/3]
[ERROR] COMPILATION ERROR :
[ERROR] .../consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[27,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
[ERROR]   required: edu.cmu.cs214.booking.BookingRequest
[ERROR]   found:    java.lang.String,long,long,<nulltype>
[ERROR]   reason: actual and formal argument lists differ in length
[ERROR] .../consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[33,19] method createBooking in interface edu.cmu.cs214.booking.BookingApi cannot be applied to given types;
[ERROR]   required: edu.cmu.cs214.booking.BookingRequest
[ERROR]   found:    java.lang.String,long,long,java.lang.String
[ERROR]   reason: actual and formal argument lists differ in length
[INFO] Reactor Summary for lab06-booking-parent 1.0.0:
[INFO] lab06-booking-parent ............................... SUCCESS
[INFO] lab06-api .......................................... SUCCESS
[INFO] lab06-consumer ..................................... FAILURE
[INFO] BUILD FAILURE
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.13.0:compile (default-compile) on project lab06-consumer: Compilation failure
```

Matches the prediction: exactly `FrontDesk.java:27` (`bookWalkIn`) and `:33` (`joinWaitlist`), at compile time.

**Which module's tests ran, and which did not.** And what that tells you about
who can detect a contract break.

`api`'s 5 tests ran and passed. `consumer`'s 7 tests never ran: its main code failed at
`compile`, so Maven never reached `testCompile` or `test`. Our green suite could not have caught
this, because I rewrote it to the new signature, so it only checks the new API. The break is visible
only from the caller's side, so only the consumer's build (the other team's code) detects it. If their
suite weren't in our build, we'd have shipped this thinking everything was green.

### Step 2: the deprecation path

**What you added.** The signatures that came back, and what they delegate to.

Two `@Deprecated` **default** methods on `BookingApi`, each with a `@deprecated` javadoc tag
naming the replacement:

```java
@Deprecated
default Booking createBooking(String roomId, long startMinute, long endMinute,
                              String waitlistKey)
    // -> createBooking(BookingRequest.of(roomId, startMinute, endMinute)
    //                      .withWaitlistKey(waitlistKey))

@Deprecated
default Booking createBooking(String roomId, long startMinute, long endMinute,
                              String waitlistKey, String notes)
    // -> createBooking(BookingRequest.of(roomId, startMinute, endMinute)
    //                      .withWaitlistKey(waitlistKey).withNotes(notes))
```

They're `default` methods, so the logic lives in one place (`createBooking(BookingRequest)`), and
an outside class implementing `BookingApi` only has to implement the new method.

**The warnings.** Paste one deprecation warning line from the build log (from
a `mvn -B clean test` run, since a rerun with nothing to compile prints none).

```
[WARNING] .../consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[27,19] createBooking(java.lang.String,long,long,java.lang.String) in edu.cmu.cs214.booking.BookingApi has been deprecated
[WARNING] .../consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:[33,19] createBooking(java.lang.String,long,long,java.lang.String) in edu.cmu.cs214.booking.BookingApi has been deprecated
```

What changed from step 1: the two `[ERROR] ... cannot be applied to given types` lines at
`FrontDesk.java:27` and `:33` became `[WARNING] ... has been deprecated` at the same line and column.
`lab06-consumer` went from `FAILURE` to `SUCCESS`, and its tests ran again:
`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`. `lab06-api` is unchanged (5/5, no warnings,
since our own code only uses the new method). Overall `BUILD SUCCESS`.

**What the deprecation path resolves.** Who can now build that could not build
during step 1, and who is on which schedule.

The front desk team (`consumer/`) builds and passes again without changing a line. So does any
other outside caller of the positional methods. We ship the new `BookingRequest` API on our schedule
today. They migrate on theirs, whenever they get to it, and until then the old calls keep working.
The old overloads come out only in a later, announced breaking release, after callers have moved.

**What the warnings accomplish that a README note would not.** Be concrete
about where the warning shows up and who sees it without looking for it.

A README note has to be found and read. The warning comes to the caller: it appears in *their* build
log, on every clean compile, naming *their* file and exact line (`FrontDesk.java:[27,19]`), and IDEs
show the call struck through. Whoever next compiles or edits `FrontDesk` sees it without looking for
it, and the `@deprecated` javadoc tells them the replacement. It also lists every remaining old call
site for them, which a README can't do.

---

## Milestone 3: The misuse critique

Not coded. One misuse, one redesign, one cost. Discuss it with your TA.

### The misuse

**What is easy to get wrong.** One specific thing about the API surface.

The bare `boolean notifyWaitlist` in `cancelBooking(long bookingId, boolean notifyWaitlist)`.
At the call site it's just `true` or `false`, so nothing says what the flag controls, and both
values type-check, so flipping one compiles fine.

**The call site.** File and line in `consumer/`, with the call. Show the
code that a reader cannot understand without opening the javadoc, or that a
caller could get wrong with the compiler still happy.

```java
// FrontDesk.java:48, cancelAndOfferToWaitlist
return api.cancelBooking(bookingId, true);

// FrontDesk.java:53, cancelQuietly
return api.cancelBooking(bookingId, false);
```

Reading `cancelBooking(bookingId, true)` alone, `true` could mean force, refund, or send email.
Only the javadoc says it means "promote the next waitlisted booking." The only thing telling the
two methods apart is one boolean literal.

**What goes wrong when it happens.** Silent bad behavior, wrong data, a crash
somewhere far away?

Silent wrong data, with no exception and no compile error. If `cancelQuietly` passed `true`, a desk
typo correction would promote a waitlisted guest to CONFIRMED, handing them a room nobody meant to
give. The reverse leaves a freed room empty while the guest stays WAITLISTED. Only a test that checks
the waitlisted booking's status afterwards would notice.

### The redesign

**The proposal.** Types, enums, factories, or whatever you are proposing. Show
the new signature and the new call site.

Replace the boolean with a two-value enum that names the behavior:

```java
public enum OnCancel {
    /** Promote the first eligible overlapping WAITLISTED booking. */
    PROMOTE_WAITLIST,
    /** Cancel without promoting anyone. */
    LEAVE_WAITLIST
}

boolean cancelBooking(long bookingId, OnCancel onCancel);
```

New call sites:

```java
api.cancelBooking(bookingId, OnCancel.PROMOTE_WAITLIST);  // FrontDesk.java:48
api.cancelBooking(bookingId, OnCancel.LEAVE_WAITLIST);    // FrontDesk.java:53
```

**Why the mistake is now hard or impossible to make.** Point at the mechanism,
such as the compiler, a validating constructor, or an exhaustive switch.

The compiler does the enforcing. The parameter type is `OnCancel`, so `true`/`false` no longer
compile, and the only values that do are named constants that say what they do. Mixing them up means
typing the wrong word, which a reviewer can see, not flipping an unlabeled literal. Inside the
implementation, a `switch (onCancel)` over the enum is checked for exhaustiveness, so a future third
option (say `NOTIFY_ONLY`) can't be silently ignored. A `null` enum still compiles, so
`cancelBooking` would throw `IllegalArgumentException` on null, and the javadoc would say so.

### One tradeoff

**What it costs.** Something real, such as caller ceremony, migration burden
against the deprecation path you just built, or more types for a newcomer to
learn. "No real downside" does not count.

Migration burden on a team we don't control, again. Removing `cancelBooking(long, boolean)` is a
breaking change just like the `createBooking` fold: `FrontDesk.java:48` and `:53` would stop
compiling. So it needs another `@Deprecated` overload (the boolean version delegating to the enum
one), and the front desk team gets a second round of migration warnings right after the
`BookingRequest` one. That's two API churns in a row for the same caller. It also adds one more type
(`OnCancel`) for a newcomer to learn, and a longer call site.

**When the price is worth paying.** A condition under which it is.

When getting the flag wrong is costly and silent, as it is here: a real guest is given or denied a
room with no error anywhere. It's also worth it while the API still has few callers, and ideally
bundled into the same deprecation cycle as the `BookingRequest` change, so callers migrate once
instead of twice. For a flag with harmless effects, or an API with many entrenched callers, the
churn may not be worth it.
