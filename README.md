AxProf
======

A framework for accuracy profiling of randomized approximate algorithm implementations. See `paper.pdf` for a full description of AxProf (to appear in ICSE 2019).

---

Directory structure
-------------------

* `AxProf` contains the source of AxProf.
* `AxProf/checkerGen` contains the checker function generator component of AxProf.
* `scripts` contains a tutorial script that uses AxProf.

---

Setup
-----

First, install the required dependencies. Assuming your system is running Ubuntu 18.04, run the following commands:

    sudo apt update
    sudo apt install python-pip python3-pip cmake build-essential python3-tk
    sudo pip install schema psutil numpy scipy pulp scikit-learn matplotlib
    sudo pip3 install mmh3 numpy scipy pulp scikit-learn matplotlib minepy

Next, run the following commands from the root directory of this repository:

    cd ./AxProf/checkerGen
    make

---

Tutorial
--------

A tutorial for using AxProf is available in `scripts/tutorial.py`