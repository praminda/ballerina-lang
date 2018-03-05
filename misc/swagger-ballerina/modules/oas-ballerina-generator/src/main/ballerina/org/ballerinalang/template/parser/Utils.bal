package org.ballerinalang.template.parser;

function splice(string[] array, int start, int end) (string[])  {
    int i = 0;
    string[] newArray = [];

    while (i <= end - start) {
        i = start == end ? 0 : i;
        newArray[i] = array[start + i];
        i = i + 1;
    }

    return newArray;
}
