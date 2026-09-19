# 17. Locators for a UI we are not allowed to change

## Status

Accepted.

## Context

Trident's reference target is a 2005 JSP application we do not modify —
[working-with-legacy.md](../working-with-legacy.md) explains why that constraint is kept even
though this particular source is available. So the suite cannot add a `data-testid`, and every
locator has to be built from markup written by someone who was not thinking about tests.

The markup was surveyed before any locator was written. Four pages, recorded rather than
assumed:

**The login form has nothing to hold on to.** The two inputs carry no `id`, no `aria-label`,
and their labels are `<p><b>Username</b></p>` elements with no `for` attribute, so neither
input has an accessible name at all:

```html
<p><b>Username</b></p>
<div class="login"><input type="text" class="input" name="username"/></div>
```

**Tables and dropdowns arrive empty.** The accounts overview is served with
`<tbody></tbody>` and filled by jQuery from a second request; the account dropdowns on the
transfer and open-account forms are served as empty `<select>` elements and populated the same
way. A locator is not the problem here — a test that starts reading as soon as the document is
complete finds an empty table and reports a missing account.

**Buttons are `<input type="submit" value="Log In">`.** For that element the `value` attribute
*is* the accessible name, so the words on the button are addressable.

## Decision

A ladder, first rung that fits wins:

1. **Accessible name** — a label, an `aria-label`, the text on a button. What the user sees.
2. **Selenium 4 relative locator** — `with(tagName("input")).below(labelElement)`. This is what
   rescues a form whose labels have no `for` attribute, and it is the only thing that addresses
   ParaBank's login fields as a user would.
3. **Structural XPath derived from an anchor** — start at a thing the scenario knows about, such
   as the link carrying its own account number, and walk to what you need.
4. **CSS or id**, only where the attribute is genuinely stable.

Two hard rules, and they are enforced rather than written down:

- **No absolute XPath.** A literal beginning `//` is anchored at the document root and breaks
  when any ancestor changes.
- **No positional index.** `[2]` or `[last()]` pins the third cell rather than the balance, and
  starts reading a different column the day someone inserts one.

`LocatorPolicyTest` reads the page-object sources and fails the build on either shape. It runs
in the Docker-free smoke suite, so the rule is checked on every build by everyone. Its own
second test feeds the scanner the two banned shapes and asserts it names them, because a guard
that cannot fail is not a guard. Shown biting: replacing `By.id("amount")` with
`By.xpath("//div[@id='transferApp']/div/form/p/input[1]")` — a locator that *works* in the
browser — fails the build with both violations named. That is the point: the build rejects it on
policy, not on behaviour, which is the only moment anyone is going to.

Rule 3 is not about locators and belongs here anyway: **every page object declares a loaded
condition, and it must be false on a half-rendered page.** `LoadablePage` waits for it before
any interaction. "The table exists" is true immediately and proves nothing; "the table has a
totals row", "the dropdown has options" are the useful shapes.

## Consequences

- A locator reads like a description of the page rather than a path through it, and survives
  the kind of markup change nobody announces.
- Relative locators cost a second query — the anchor, then the element — and they are slower
  and harder to debug than an id would be. On the login form there is no id to prefer.
- Steps contain no `By` at all. That is the technical form of CONTRIBUTING's promise that
  nobody is asked to maintain locators in step definitions, and CI greps every `steps` and
  `websteps` package — including the archetype's templates — to keep it true. This record said
  "checked by grep" for a phase while the grep was something a human ran, which is the same
  shape of overclaim a pre-tag review caught in [ADR 0011](0011-readiness-probe-triggers-schema-init.md).
- The scanner reads string literals, so a locator assembled at run time from pieces slips past
  it. Known and accepted: the point is to stop the easy mistake, and anyone concatenating their
  way around it has stopped making it by accident.
- It walks the page-object package recursively. It did not at first — it listed one directory —
  so moving an offending locator into a subpackage satisfied it, which is where page objects go
  as soon as there are more than a handful. A guard that can be satisfied by moving the problem
  is not a guard, and there is now a case asserting it reaches a package one level down.

## Alternatives rejected

**Add test ids to ParaBank.** One attribute per field and the whole problem disappears. Rejected
because it is the one thing this project has decided it cannot do — see
[working-with-legacy.md](../working-with-legacy.md#we-do-not-modify-the-application-under-test).
In the jobs this framework is for, the application belongs to another team, another company, or
is a vendor binary, and a suite that only works after someone patches the product does not
survive contact with a real client.

**Worth recording as the contrast, because the rule inverts:** where the application *can* be
changed, stable accessibility identifiers are the right answer and everything above is a
workaround. A team that owns its front end should add them and locate by them, and should treat
a relative locator as a sign that something needs a label. The strategy here is a response to a
constraint, not a general preference.

**Id-first locators.** This was the expected answer, on the stated grounds that ParaBank's ids
are generated from the data and therefore change every run. **That premise was checked and it
is false.** Every `id` attribute across the five pages surveyed is written into the JSP by hand
— `accountTable`, `fromAccountId`, `type`, `showResult`, `rightPanel` — and none is derived from
a customer or an account. What *is* data-derived is the account number itself, which appears as
link text, as an option value and in an `href`, never as an id.

The rejection stands on different evidence than expected:

- **The fields that matter have no id.** Both login inputs have only a `name`. An id-first
  strategy has nothing to offer on the one form every scenario goes through.
- **One id is not unique.** The transfer page renders `<p id="amount.errors">` **twice**, so
  `By.id` there is ambiguous — and the dot makes `By.cssSelector("#amount.errors")` mean "id
  `amount` and class `errors`", which matches nothing at all. Two different traps in one
  attribute.
- **Ids are on containers, not on what a scenario asserts about.** No row, cell or account link
  carries one, so the balance of a particular account is unreachable by id whatever the page
  does.

So ids are used where they exist and are stable — `#type`, `#fromAccountId`, `#accountTable` —
and the ladder does the work where they do not. Recording the correction rather than the
expectation is the same discipline as [ADR 0016](0016-a-controlled-variable-that-never-varies.md):
a rule kept for a reason that turns out to be untrue is a rule nobody can apply correctly next
time.

**Page Factory with `@FindBy`.** Selenium's annotation-driven page objects, which would remove
some of the boilerplate. Rejected because its proxies re-find elements lazily on every call,
which interacts badly with explicit waits and hides where a wait actually happens — and because
it cannot express a relative locator, which is rung 2 and the rung this application needs most.

**A shared locator repository, in properties or YAML.** Popular, and it moves every selector out
of Java. Rejected because it buys one kind of tidiness with two costs: no compiler, and no place
to write down why a locator is shaped the way it is. The comments at the locators here — which
rung, and what was measured — are the part a future reader needs, and a properties file has
nowhere to put them.
