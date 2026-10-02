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

**Which module's tests ran, and which did not.** And what that tells you about
who can detect a contract break.

### Step 2: the deprecation path

**What you added.** The signatures that came back, and what they delegate to.

**The warnings.** Paste one deprecation warning line from the build log (from
a `mvn -B clean test` run, since a rerun with nothing to compile prints none).

**What the deprecation path resolves.** Who can now build that could not build
during step 1, and who is on which schedule.

**What the warnings accomplish that a README note would not.** Be concrete
about where the warning shows up and who sees it without looking for it.

---

## Milestone 3: The misuse critique

Not coded. One misuse, one redesign, one cost. Discuss it with your TA.

### The misuse

**What is easy to get wrong.** One specific thing about the API surface.

**The call site.** File and line in `consumer/`, with the call. Show the
code that a reader cannot understand without opening the javadoc, or that a
caller could get wrong with the compiler still happy.

**What goes wrong when it happens.** Silent bad behavior, wrong data, a crash
somewhere far away?

### The redesign

**The proposal.** Types, enums, factories, or whatever you are proposing. Show
the new signature and the new call site.

**Why the mistake is now hard or impossible to make.** Point at the mechanism,
such as the compiler, a validating constructor, or an exhaustive switch.

### One tradeoff

**What it costs.** Something real, such as caller ceremony, migration burden
against the deprecation path you just built, or more types for a newcomer to
learn. "No real downside" does not count.

**When the price is worth paying.** A condition under which it is.
