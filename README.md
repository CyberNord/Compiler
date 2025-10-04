# MicroJava Compiler

## Overview
This project is a **from-scratch compiler** for the teaching language **MicroJava**, built as part of a compiler construction exercise.  
It implements the complete compiler pipeline:

- **Scanner (lexical analysis)**
- **Parser (syntax analysis)**
- **Symbol table & types** (`TabImpl`, `StructImpl`)
- **Semantic checks & error reporting**
- **Code generation** to MicroJava bytecode
- **Bytecode runner (VM / interpreter)** to execute compiled programs

---

## Tech stack
- **Language:** Java  
- **Tests:** JUnit 4  
- **Project layout:** plain Java sources (no Maven/Gradle); IDE files for IntelliJ/Eclipse included.

---

## Requirements

> Requires JDK 8+



## Feedback

 - Exercise 2: Scanner	                            16,67 %	24,00	0–24	100,00 %	ok

 - Exercise 3: Parser	                            16,67 %	24,00	0–24	100,00 %	ok

 - Exercise 4: Symbolliste und Fehlerbehandlung	16,67 %	22,50	0–24	 93,75 %	MH
            TabImpl:
                77: if there is no name(null or "") return noObj
            Parser:
                250: check followers, as there could somewhere be an extra scan after an error -0,5
                542, 550: you should also synchronize on name that refers to types -1

 - Exercise 5: Codeerzeugung Teil 1 (Operands)	    16,67 %	23,00	0–24	95,83 %	
            CodeImpl: 
                164: do checks in parserImpl and throw errors there ; 
                Also i dont think you need a method for doBasicArithmetic at all, just put it into the parser. 
                Same for doPrint and getArray. But as said, it is just my taste 
                - BUT errors should indeed just be thrown in parserImpl -1.

 - Exercise 6: Codeerzeugung Teil 2 (Sprünge)	    16,67 %	23,50	0–24	97,92 %	    MH
            LabelImpl:
                19: make private -0,5
            CodeImpl: ok
            ParserImpl: ok

------------------------------------------------------------------------------------------------------------------------

All Errors were corrected.   

------------------------------------------------------------------------------------------------------------------------

created by CyberNord 2021 
 
DO NOT COPY MY CODE FOR SUBMISSIONS! 

                
