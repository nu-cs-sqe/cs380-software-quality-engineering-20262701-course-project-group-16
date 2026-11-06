# Instructor Code Review Feedback

**Contact**: Dr. Yiji Zhang (yiji.zhang@northwestern.edu)

**Purpose of This Document**:
The instructor will perform code review with respect to software design, error handling, format and style on the main branch every week starting Week 6 using the letter grade A standards.
The following chapters of the textbook are considered: Chapter 1, 2, 3, 4, 5, 6, 7, 9, and 10. The corresponding lectures are considered, too.


## Week 6 Code Review
I have read every line of production code currently in the main branch. A couple of comments:
1. GameInitializer#start method is too big. Every for loop probably can be put into a private method with a meaningful method name indicating what the computation is for. Check other classes and methods to see whether such refactoring can be applied, too. For instance, the if statement in GameConfig constructor should be its own private method too, as Uncle Bob has a very similar example in the textbook.
2. GameInitializer#start method is not unit-testable because of the instantiation of list of Card and PlayerState. Your GameInitializerTest is not unit tests, but integration tests. Check your other classes and methods too.
3. Null check: your Player class contains null check. So here are a couple of things to consider:
1) indeed, it is generally a good practice to do "defense programming". Your null check is a great example.
2) However, what uncle Bob argues in the book is that instead of doing null check everywhere, especially in a language like Java, we should make sure we don't pass null around, period.
3) If you work on an API or a library that is designed to be used by others, the null check is more important. Or, if you work in a language like C or C++ where there is pointer allowed, it is more important to check for null explicitly and thoroughly. 
So, what's the conclusion? In general, for this course project, since you are developing a standalone application in Java, it is less important to do null check. Instead, we should focus on making sure we don't pass null around.
4. Your Localizations#collectArgumentTypes method most likely will not pass Google or Oracle's line width rule after you set up Checkstyle. You want to break it into smaller methods with better readability.

A lot more progress compared to last time. Good job! Make sure you do a thorough code review of the code generated.    
