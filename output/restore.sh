#!/bin/sh

for i in {0..11}
do
    cat apiSequences.json.${i}-* > apiSequences.json.${i}
done

