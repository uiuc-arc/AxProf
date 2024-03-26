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

First, install the required dependencies. Assuming your system is running Ubuntu 20.04 and Python 3.8, run the following commands:

    sudo apt update
    sudo apt install build-essential default-jdk python3-pip
    sudo python3 -m pip install numpy==1.24.3 scipy==1.10.1 matplotlib==3.7.1 minepy==1.2.6

Note that newer versions of Python 3 (e.g., 3.11) may not support AxProf. Use `pyenv` if necessary to run AxProf in a Python 3.8 environment.
Next, run the following commands from the root directory of this repository:

    cd AxProf/checkerGen
    make

---

Tutorial
--------

A tutorial for using AxProf is available in `tutorial/tutorial.py`.

---

Example
-------

An example script for testing [`ekzhu/datasketch`](https://github.com/ekzhu/datasketch) is provided in `examples/hllEkzhu.py`. To run the script, you must first clone the `datasketch` repository. Run the following commands from the root directory of this repository:

    cd examples
    git clone https://github.com/ekzhu/datasketch.git

Now you can run `examples/hllEkzhu.py` to test the library.
