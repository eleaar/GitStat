
GitStat
=======

Browse stats about your favourite GitHub repositories.

[![Build Status](https://travis-ci.org/eleaar/GitStat.svg?branch=master)](https://travis-ci.org/eleaar/GitStat)
[![Deploy](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)


![](https://github.com/eleaar/GitStat/blob/master/images/Atomic.PNG)

![](https://github.com/eleaar/GitStat/blob/master/images/Cached.PNG)


Throughput [op/s]

| Users | default | atomic |
|-------|---------|--------|
| 10    | 16      | 22     |
| 50    | 16      | 106    |
| 100   | 16      | 216    |
| 500   | 16      | 1002   |

Avg Response Time [s]

| Users | default | atomic |
|-------|---------|--------|
| 10    | 622     | 443    |
| 50    | 3000    | 448    |
| 100   | 5487    | 435    |
| 1000  | 28458   | 480    |
