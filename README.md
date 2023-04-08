# bibliography-sort

to sort bibliography execute `java -cp out/artifacts/bibliography_sort_jar/bibliography-sort.jar Main` and pass one or two arguments.

arguments:
 * <path> - path to dir with files biblio.bib and bibliography.tex
 * -f - flag that indicates that in bibliography.tex does not contain entry for a valid entry from biblio.bib then sorted file should contain that entry created by template 
 ```scnciteheader{<header>}
\scnfullcite{<header>}
\scnciteannotation{<header>}
\begin{scnreltolist}{библиографическая ссылка}
  \scnitem{\ref{} \nameref{}}
\end{scnreltolist}
```
some errors are printed in console for manual fixes
