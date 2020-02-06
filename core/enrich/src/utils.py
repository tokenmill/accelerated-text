import logging
import re

import spacy

from collections import Counter, defaultdict
from functools import reduce

logger = logging.getLogger("Utils")


class OpRejected(RuntimeError):
    pass


def sliding_window(lst, n):
    for i in range(0, len(lst) - n):
        yield lst[i:i+n]


def inside(lst1, lst2):
    cond = lambda subset: subset == lst1
    n = len(lst1)
    for subset in sliding_window(lst2, n):
        if subset == lst1:
            return True

    return False



def load_seq():
    with open("data/text_raw.txt", "r") as f:
        return [t.replace(".", "").strip()
                for t in tokenize(f.read())]


def load_example_triplets():
    with open("data/text_raw.txt", "r") as f:
        return Counter(ngram(load_seq(), n=3))


def tokenize(text):
    def split_token_further(t):
        buff = re.split(r"[.,:!?]", t)
        for b in buff:
            yield b

    return [ts
            for t in re.split(r"\s", text.lower())
            for ts in split_token_further(t)
            if t.strip() != ""]


def ngram(g, n):
    if n > 1:
        return zip(*[g[i:] for i in range(n)])
    else:
        return g



def left_pair(k1, k2, triplets):
    """
    We have left word and searching for right
    (<our_words>, variants...)
    """
    rights = [(r, v)
              for (l, m, r), v in triplets.items() 
              if l == k1 and m == k2]
    total = sum([v for (_, v) in rights])
    return sorted([(r, v/total) for (r, v) in rights], key=lambda x: -x[1])


def right_pair(k1, k2, triplets):
    """
    We have right word and searching for left
    (variants..., <our_words>)
    """
    lefts = [(l, v)
              for (l, m, r), v in triplets.items() 
              if m == k1 and r == k2]
    total = sum([v for (_, v) in lefts])
    return sorted([(r, v/total) for (r, v) in lefts], key=lambda x: -x[1])


def middle(k1, k2, triplets):
    """
    Searching for a word in between
    """
    middles = [(m, v)
                for (l, m, r), v in triplets.items() 
                if l == k1 and r == k2]
    total = sum([v for (_, v) in middles])
    return sorted([(r, v/total) for (r, v) in middles], key=lambda x: -x[1])


def window(lefts, rights, triplets):
    logger.debug("Lefts: {0}, Rights: {1}".format(lefts, rights))
    if len(lefts) == 1 and len(rights) == 1:
        return middle(lefts[0], rights[0], triplets)
    elif len(lefts) == 0 and len(rights) == 1:
        return []
    elif len(lefts) == 1 and len(rights) == 1:
        return []
    elif len(lefts) == 0 and len(rights) == 2:
        return right_pair(rights[0], rights[1], triplets)
    elif len(lefts) == 2 and len(rights) == 0:
        return left_pair(lefts[0], lefts[1], triplets)
    else:
        logger.debug("Lefts: {0}, Rights: {1}".format(lefts, rights))
        results = defaultdict(list)
        middle_results = [(k, v)
                          for (k, v) in middle(lefts[-1], rights[0], triplets)
                          if k not in lefts+rights]
        logger.debug("Middle results: {}".format(middle_results))
        for (m, p) in middle_results:
            results[m].append(p)
            
        if len(lefts) == 2:
            logger.debug("Handling left: {}".format(lefts))
            left_results = [(k, v)
                            for (k, v) in left_pair(lefts[0], lefts[1], triplets)
                            if k != rights[0]]
            logger.debug("Left results: {}".format(left_results))
            for (l, p) in left_results:
                results[l].append(p)
                
        if len(rights) == 2:
            logger.debug("Handling right: {}".format(rights))
            right_results = [(k, v)
                             for (k, v) in right_pair(rights[0], rights[1], triplets)
                             if k != lefts[-1]]
            logger.debug("Right results: {0}, lefts: {1}".format(right_results, lefts[-1]))
            for (r, p) in right_results:
                results[r].append(p)

        def avg(lst):
            n = len(lst)
            return sum(lst)/n

        results_normalized = [(k, avg(v))for k, v in results.items()]
                
        return sorted(results_normalized, key=lambda x: -x[1])[:10]
    

def is_placeholder(t):
    return t.startswith("{")


def get_pos_signature(tokens, nlp=None):
    doc = nlp(" ".join(tokens))
    pattern = [(t.text, t.pos_) for t in doc]

    def gen():
        prev = None
        for (t, p) in pattern:
            if prev == "placeholder_start":
                yield "VARIABLE"
                prev = None
                continue
            
            if t == "{":
                prev = "placeholder_start"
            elif t == "}":
                prev = "placeholder_end"
            else:
                prev = None
                if p not in ["AUX"]:
                    yield p

    return list(gen())

def grammatically_valid_pos(pos):
    pairs = list(ngram(pos, n=2))
    if any([p1 == "DET" and p2 == "VERB" for (p1, p2) in pairs]):
        logger.debug("DET before VERB")
        return False

    if any([(p1 == "ADV" and p2 == "ADP") or (p1 == "ADP" and p2 == "ADV")
            for (p1, p2) in pairs]):
        logger.debug("ADV and ADP in pair")
        return False
    return True

def compare_pos_signatures(left, right):
    def filter_fn(p):
        return p not in ["DET", "ADV"]

    left_side = list(filter(filter_fn, left))
    right_side = list(filter(filter_fn, right))
    return left_side == right_side


def validate(original, new, nlp):
    placeholders_original = sum([1 for t in original if is_placeholder(t)])
    placeholders_new = sum([1 for t in new if is_placeholder(t)])
    print("Orig: {0}, New: {1}".format(placeholders_original, placeholders_new))
    if placeholders_original != placeholders_new :
        raise OpRejected("New placeholders introduced or removed")

    orig_pos = get_pos_signature(original, nlp)
    new_pos = get_pos_signature(new, nlp)

    if not compare_pos_signatures(orig_pos, new_pos):
        raise OpRejected("Lexical Structure changed too much")

    if not grammatically_valid_pos(new_pos):
        raise OpRejected("Invalid gramatical structure")

    return new


def insert(t, pos, triplets):
    left_side = t[max(0, pos-2):pos]
    right_side = t[pos:pos+2]

    results = window(left_side, right_side, triplets)
    if len(results) == 0:
        raise OpRejected("Couldn't insert anything")
    else:
        (m, _) = results[0]
        logger.debug("Left side: {0}, Right side: {1}, Variants: {2}".format(t[:pos], t[pos:], results))
        return t[:pos] + [m] + t[pos:]


def remove(t, pos):
    if is_placeholder(t[pos]):
        raise OpRejected("Don't remove placeholders")

    result = t
    del result[pos]
    return result


def replace(t, pos, triplets):
    if is_placeholder(t[pos]):
        # Don't replace placeholders
        raise OpRejected("Don't replace placeholders")

    new = t
    
    left_side = t[:pos][-2:]
    right_side = t[pos+1:][:2]
    current = t[pos]
    
    results = window(left_side, right_side, triplets)
    if len(results) == 0:
        raise OpRejected("Nothing to replace")
    else:
        (m, _) = list([(m, p) for m, p in results if m != current])[0]
        new[pos] = m
        return new


def format_result(text):
    def strip(t):
        return t.strip()
    
    def capitalize_first(t):
        return t[0].upper() + t[1:]

    def end_with_dot(t):
        return t + "."

    pipeline = [strip, capitalize_first, end_with_dot]
    return reduce(lambda t, fn: fn(t), pipeline, text)
