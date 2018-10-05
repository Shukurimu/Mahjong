# Mahjong

Japanese Mahjong implementation using JavaFX


Picture resources https://zh.wikipedia.org/wiki/%E5%9B%BD%E6%A0%87%E9%BA%BB%E5%B0%86

Reference website http://www.abstreamace.com/mahjong/



> Demo of current process (jar file)
> - [20180926](https://drive.google.com/file/d/1HrD-T9F4EUn4QeD1a0bQxRh6Ex-5HK7r/view?usp=sharing)
> - Require JRE 10 or later version.



### Complie

`javac -encoding utf8 *.java`



### Misc

I know that it should be called *tile* instead of *card*.  
But it's more straightforward to me, though.



### Update History

- 201808~20180923 framework
- 20180926  
    - fixed some Yaku verifying bugs  
    - updated the judgement of Ankan after richi  
    - added more comments and renamed some variables to be more self-explainable  
    - changed Card fxNode form ImageView to Button (user can press `SPACE` to perform default action now)  
    - added logger functionality for game recoding (and debugging)
    - added declaration text for player actions  
    - updated the judgement of Ankan after richi  
