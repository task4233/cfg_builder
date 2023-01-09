#!/bin/sh

for file in apiSequences.json.*
do
    # remove
    echo "split -b 32M -d ${file} ${file}-"
    split -b 32M -d ${file} ${file}-

    # split
    rm ${file}
done

