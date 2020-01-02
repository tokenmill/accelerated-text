import sys
import csv
import random

import requests

# Hack to add custom path
sys.path.append("e2e-metrics")

from itertools import dropwhile, takewhile, groupby

from metrics.pymteval import BLEUScore

DOCUMENT_PLAN_ID="2abf24fc-a52f-42b6-875d-8a41562fcc38"
NLG_ENDPOINT="http://localhost:3001/nlg"


def bleu_score(data):
    bleu = BLEUScore()
    for ref, base in data:
        bleu.append(base, ref)

    return bleu.score()


def not_empty_line(x):
    return x != "\n"

def generate_results(data):
    req = {
        "documentPlanId": DOCUMENT_PLAN_ID,
        "readerFlagValues": {},
        "dataRows": data
    }

    resp = requests.post("{url}/_bulk/".format(url=NLG_ENDPOINT), json=req)
    result_id = resp.json()["resultId"]
    print("ResultId: {}".format(result_id))

    results = requests.get("{url}/{result_id}?format=raw".format(
        url=NLG_ENDPOINT,
        result_id=result_id
    )).json()

    return results["variants"]


def load_data():
    with open("data/devset.csv", "r") as f:
        reader = csv.DictReader(f)
        for row in reader:
            item = {"ref": row["ref"]}
            row.pop("ref")
            item["data"] = row
            yield item

def group_data(data):
    return [(k, list([item["ref"] for item in group]))
            for k, group in groupby(data, key=lambda x: x["data"])]
    

if __name__ == "__main__":
    ref = []
    data_rows = {}

    items = list(group_data(load_data()))

    for idx, (data, refs) in enumerate(items):
        ref.append(refs)
        data_rows[idx] = data

    results = generate_results(data_rows)
    pairs = list([(ref[int(k)], random.choice(r))
                  for k, r in results.items()])

    score = bleu_score(pairs)
    print("BLEU score: {0:.4f}".format(score))
