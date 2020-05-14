"""PYX-Reloaded Card Loader

This is a short-term hack that I'm using to load cards into the database for
PYX-Reloaded. It reads a JSON manifest and then inserts the cards into the
SQLite database.
"""

import argparse
import json
import logging
import os
import re
import sqlite3
from typing import Dict, Any

logging.basicConfig()
logger = logging.getLogger(__name__)

BLANK_RE = re.compile("____+")

def serial_hack(x: int):
    n = x
    while True:
        n = n + 1
        yield n


def main(filename: str, dbname: str) -> None:
    """Load a deck into the database."""
    dirname = os.path.dirname(filename)

    data = {}  # type: Dict[str, Any]
    with open(filename) as f:
        data = json.load(f)

    white_cards = []  # type: List[str]
    black_cards = []  # type: List[str]

    with open(os.path.join(dirname, data["cards"]["white"]["file"])) as f:
        white_cards = [x.strip() for x in f.readlines()]

    with open(os.path.join(dirname, data["cards"]["black"]["file"])) as f:
        black_cards = [x.strip() for x in f.readlines()]
        black_cards = [BLANK_RE.sub("____", x) for x in black_cards]

    set_description = data["description"]  # type: str
    set_name = data["name"]  # type: str
    watermark = data.get("watermark", "")  # type: str

    with sqlite3.connect(dbname) as conn:
        c = conn.cursor()

        # I can't believe it's 2020 and I'm being forced to manually manage keys
        card_set_seq = serial_hack(
            c.execute("select max(id) from card_set").fetchone()[0]
        )
        black_cards_seq = serial_hack(
            c.execute("select max(id) from black_cards").fetchone()[0]
        )
        white_cards_seq = serial_hack(
            c.execute("select max(id) from white_cards").fetchone()[0]
        )

        card_set_id = next(card_set_seq)
        card_set_data = (card_set_id, True, set_name, False, set_description)
        c.execute(
            """insert into card_set(id, active, name, base_deck, description)
                     values (?, ?, ?, ?, ?)""",
            card_set_data,
        )
        conn.commit()

        white_card_tuples = []
        white_card_set_tuples = []
        for card in white_cards:
            card_id = next(white_cards_seq)
            white_card_tuples.append((card_id, card, watermark))
            white_card_set_tuples.append((card_set_id, card_id))

        c.executemany(
            "INSERT INTO white_cards (id, text, watermark) VALUES (?, ?, ?)",
            white_card_tuples,
        )
        c.executemany(
            "INSERT INTO card_set_white_card (card_set_id, white_card_id) VALUES (?, ?)",
            white_card_set_tuples,
        )

        black_card_tuples = []
        black_card_set_tuples = []
        for card in black_cards:
            pick = max(len(card.split("____")) - 1, 1)
            draw = 0
            if pick >= 3:
                draw = pick - 1

            card_id = next(black_cards_seq)
            black_card_tuples.append((card_id, card, watermark, draw, pick))
            black_card_set_tuples.append((card_set_id, card_id))

        c.executemany(
            "INSERT INTO black_cards (id, text, watermark, draw, pick) VALUES (?, ?, ?, ?, ?)",
            black_card_tuples,
        )
        c.executemany(
            "INSERT INTO card_set_black_card (card_set_id, black_card_id) VALUES (?, ?)",
            black_card_set_tuples,
        )

        conn.commit()
    pass


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Load decks into PYX")
    parser.add_argument(
        "-d",
        "--database",
        dest="db",
        action="store",
        help="Filename of SQLite database for PYX",
        default="pyx.sqlite",
    )
    parser.add_argument(
        "filename", nargs=1, action="store", help="JSON manifest to load cards from",
    )

    args = parser.parse_args()

    print(args)
    main(args.filename.pop(), args.db)
