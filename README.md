AxProf
======

A framework for accuracy profiling of randomized approximate algorithm implementations. See `ICSE-2019-Paper.pdf` for a full description of AxProf (to appear in ICSE 2019).

---

Directory structure
-------------------

* `AxProf` contains the source of AxProf.
* `AxProf/checkerGen` contains the checker function generator component of AxProf.
* `tutorial` contains a tutorial script that uses AxProf.
* `examples` contains example scripts for testing some of the benchmarks from the conference paper.

---

Setup
-----

First, install the required dependencies. Assuming your system is running Ubuntu 18.04, run the following commands:

    sudo apt update
    sudo apt install python-pip python3-pip cmake build-essential python3-tk
    sudo pip install schema psutil numpy scipy scikit-learn matplotlib
    sudo pip3 install mmh3 numpy scipy pulp scikit-learn matplotlib minepy

Next, run the following commands from the root directory of this repository:

    cd ./AxProf/checkerGen
    make

---

Tutorial
--------

A tutorial for using AxProf is available in `tutorial/tutorial.py`

---

Example
-------

An example script for testing [`ekzhu/datasketch`](https://github.com/ekzhu/datasketch) is provided in `examples/hllEkzhu.py`. To run the script, you must first clone the `datasketch` repository. Run the following commands from the root directory of this repository:

    cd examples
    git clone https://github.com/ekzhu/datasketch.git

Now you can run `examples/hllEkzhu.py` to test the library.