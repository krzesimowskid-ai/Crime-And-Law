#!/usr/bin/env python3
"""Check and simulate the territory design; does not read or modify a game world.

Run from any working directory. All results are derived from balance/territory-v1.json.
Only Python's standard library is required.
"""

import argparse
import json
from fractions import Fraction
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BASIS = 10_000


def require(condition, message):
    if not condition:
        raise ValueError(message)


def accrue(base_units, multiplier_bp, remainder=0):
    """Return whole units and retained fractional units, without floating point."""
    require(type(base_units) is int and base_units >= 0, "Negative/non-integer base")
    require(type(multiplier_bp) is int and multiplier_bp >= 0, "Invalid multiplier")
    require(type(remainder) is int and 0 <= remainder < BASIS, "Invalid remainder")
    return divmod(base_units * multiplier_bp + remainder, BASIS)


def money(cents):
    sign = "-" if cents < 0 else ""
    cents = abs(cents)
    whole = f"{cents // 100:,}".replace(",", " ")
    return f"{sign}{whole},{cents % 100:02d} $"


def decimal(value):
    return f"{float(value):.2f}".replace(".", ",")


def inspect_balance(data):
    require(data["status"] == "design_draft_not_loaded_by_mod", "Design status changed")
    require(data["currency"]["minor_units_per_unit"] == 100, "Currency precision changed")
    qualification = data["qualification"]
    require(not qualification["ordinary_chunk_has_bonus"], "Ordinary chunks award bonuses")
    require(not qualification["ordinary_chunk_has_levels"], "Ordinary chunks have bonus levels")
    require(qualification["specific_permanent_qualification_required"], "Qualification missing")
    require(qualification["coarse_traits_are_not_qualification"], "Coarse traits grant bonuses")
    require(qualification["generated_buildings_only"], "Player builds create bonus sources")
    require(qualification["deduplicate_generated_source_across_chunks"], "Duplicate sources allowed")
    economy = data["economy"]
    require(type(economy["reference_hourly_base_reward_cents"]) is int
            and economy["reference_hourly_base_reward_cents"] > 0,
            "Expected positive reference income in whole cents")
    require(economy["money_requires_verified_local_activity"], "Passive payment enabled")
    require(not economy["offline_money_generation"], "Offline payment enabled")
    require(economy["same_event_same_effect_combination"] == "maximum", "Bonus stacking changed")
    require(economy["rounding"] == "carry_fractional_minor_units", "Fractions discarded")
    levels = data["progression"]["base_levels"]
    require(levels == 5, "Expected five base levels")
    require(not data["progression"]["special_level_6_enabled"], "Exceptional sixth level enabled")
    hours = data["progression"]["minimum_control_hours"]
    costs = data["progression"]["upgrade_cost_as_hourly_base_reward_bp"]
    for name, values in (("hours", hours), ("costs", costs)):
        require(len(values) == levels, f"Wrong {name} count")
        require(all(type(v) is int and v >= 0 for v in values), f"Invalid {name}")
        require(values[0] == 0, f"Initial {name} must be zero")
        require(all(a < b for a, b in zip(values, values[1:])), f"Non-increasing {name}")
    for name, curve in data["curves"].items():
        values, cap = curve["values_bp"], curve["cap_bp"]
        require(len(values) == levels, f"Wrong curve length: {name}")
        require(all(type(v) is int and v > 0 for v in values), f"Invalid curve: {name}")
        require(all(a < b for a, b in zip(values, values[1:])), f"Non-increasing curve: {name}")
        require(type(cap) is int and max(values) <= cap, f"Curve exceeds cap: {name}")
        require(curve["kind"] in ("increase", "reduction", "multiplier"), f"Unknown curve: {name}")
        if curve["kind"] == "reduction":
            require(cap < BASIS, f"Reduction can remove all cost/time: {name}")
    require("transport" in data["deferred_systems"], "Transport must remain deferred")
    require("sidebar_premie" in data["deferred_systems"], "Premie category must remain separate")


def simulate(data):
    """Exercise economic consequences, including fragmented and duplicate events."""
    curve = data["curves"]["activity_money"]
    levels = curve["values_bp"]
    cap = curve["cap_bp"]
    checks = []
    for count in (1, 12, 100):
        # Ownership alone has no reward event; zero input remains zero.
        value, remainder = accrue(0, levels[-1])
        require(value * count == remainder == 0, "Ordinary/idle land generates money")
    checks.append("1, 12 i 100 zwykłych lub bezczynnych chunków: 0,00 $ dochodu")

    # Same local reward event can be affected by several overlapping sources.
    # It is evaluated once, with the strongest applicable effect, not multiplied.
    for copies in (1, 2, 12, 100):
        result, remainder = accrue(10_000, min(cap, max([levels[-1]] * copies)))
        require((result, remainder) == (16_000, 0), "Duplicate effects multiply income")
    checks.append("1–100 nakładających się źródeł jednego efektu: najwyżej mnożnik 1,60")

    # Splitting the same activity budget across many areas must not mint money.
    for count in (1, 12, 100):
        whole, remainder = 0, 0
        pieces = [10_003 // count + (i < 10_003 % count) for i in range(count)]
        for piece in pieces:
            awarded, remainder = accrue(piece, levels[-1], remainder)
            whole += awarded
        require((whole, remainder) == accrue(10_003, levels[-1]), "Splitting activity changes payout")
    checks.append("Ten sam obrót rozdzielony na 1, 12 lub 100 terenów: identyczna wypłata")

    # Low-value actions retain their fractional payout until it becomes payable.
    fractional_actions = 0
    for curve_data in data["curves"].values():
        for multiplier in curve_data["values_bp"]:
            whole, remainder = 0, 0
            for _ in range(10_003):
                awarded, remainder = accrue(1, multiplier, remainder)
                whole += awarded
            require((whole, remainder) == accrue(10_003, multiplier), "Fractions are lost or created")
            fractional_actions += 10_003
    checks.append(f"{fractional_actions:,} małych naliczeń: brak strat lub nadpłat przez zaokrąglenia".replace(",", " "))

    # A cooldown or price remains positive at every level; speed is not time cut.
    for curve_data in data["curves"].values():
        if curve_data["kind"] == "reduction":
            for value in curve_data["values_bp"]:
                require(Fraction(BASIS - value, BASIS) > 0, "Free/infinite action possible")
    require(Fraction(BASIS, BASIS + 1500) == Fraction(20, 23), "Growth conversion error")
    checks.append("Obniżki pozostawiają dodatni koszt/czas; +15% tempa nie oznacza −15% czasu")

    base = data["economy"]["reference_hourly_base_reward_cents"]
    costs = data["progression"]["upgrade_cost_as_hourly_base_reward_bp"]
    rows = []
    previous = None
    previous_payback = Fraction(0)
    for i, multiplier in enumerate(levels):
        income = Fraction(base * multiplier, BASIS)
        cost = Fraction(base * costs[i], BASIS)
        require(income.denominator == cost.denominator == 1, "Reference table requires whole cents")
        payback = None if previous is None else cost / (income - previous)
        if payback is not None:
            require(payback > previous_payback, "Higher upgrades repay faster than lower upgrades")
            previous_payback = payback
        rows.append((i + 1, int(income), int(cost), payback))
        previous = income
    checks.append("Zwrot kosztu kolejnego ulepszenia: 4 / 8 / 16 / 24 godziny przy ruchu referencyjnym")
    return rows, checks


def report(data, rows, checks):
    text = [
        "# Crime And Law — symulacja balansu terytoriów v1",
        "",
        "Status: **propozycja projektowa, nieaktywna w JAR-ze 0.1.15**.",
        "Wyniki poniżej pochodzą z modelu; nie są testem gry ani gospodarki serwera Majestic.",
        "",
        "## Punkt dochodowy — przykład referencyjny",
        "",
        "Baza: 100,00 $ za godzinę kwalifikowanej aktywności. Nie jest to wypłata za sam upływ czasu.",
        "Koszt poziomu 1 w tabeli wynosi 0,00 $, ponieważ zdobycie terenu jest osobną czynnością.",
        "Kwoty skalują się z bazą danego źródła; nie są jedną ceną dla wszystkich obiektów.",
        "",
        "| Poziom | Przychód / godzinę aktywności | Koszt tego ulepszenia | Min. kontroli łącznie | Zwrot z dodatkowego przychodu |",
        "|---|---:|---:|---:|---:|",
    ]
    for level, income, cost, payback in rows:
        duration = data["progression"]["minimum_control_hours"][level - 1]
        text.append(f"| {level} | {money(income)} | {money(cost)} | {duration} h | {'—' if payback is None else decimal(payback) + ' h'} |")
    text.extend([
        "",
        "Zwrot uwzględnia tylko różnicę przychodu po ulepszeniu, nie cały przychód obiektu.",
        "Przy połowie aktywności czasy zwrotu wynoszą 8 / 16 / 32 / 48 godzin,",
        "a przy podwójnej aktywności 2 / 4 / 8 / 12 godzin. Brak aktywności oznacza brak zwrotu.",
        "Cena źródła, pełne wydatki operacyjne i popyt wymagają osobnego modelu gospodarki.",
        "",
        "## Scenariusze obliczeniowe",
        "",
        *[f"- PASS — {check}." for check in checks],
        "",
        "To sprawdza model matematyczny. Zapobieganie powtórnym transakcjom, zapis ułamków,",
        "autoryzacja, ocena terenu i wypłaty serwera nadal wymagają kodu oraz testów integracyjnych.",
        "Nie należy traktować tej symulacji jako potwierdzenia ich działania w Minecrafcie.",
        "",
    ])
    return "\n".join(text)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", type=Path, default=ROOT / "balance/territory-v1.json")
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    data = json.loads(args.config.read_text(encoding="utf-8"))
    inspect_balance(data)
    rows, checks = simulate(data)
    result = report(data, rows, checks)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(result, encoding="utf-8")
    print(result)


if __name__ == "__main__":
    main()
