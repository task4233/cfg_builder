#!/bin/sh

# split
split -b 32M -d apiSequences.json.0 apiSequences.json.0-
split -b 32M -d apiSequences.json.1 apiSequences.json.1-
split -b 32M -d apiSequences.json.2 apiSequences.json.2-
split -b 32M -d apiSequences.json.3 apiSequences.json.3-
split -b 32M -d apiSequences.json.4 apiSequences.json.4-
split -b 32M -d apiSequences.json.5 apiSequences.json.5-

# remove
rm apiSequences.json.0
rm apiSequences.json.1
rm apiSequences.json.2
rm apiSequences.json.3
rm apiSequences.json.4
rm apiSequences.json.5

