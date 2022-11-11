# Projet : Exercice SATPlanner

Marine Troccaz, Nicolas Girardier

## L'analyse du projet

Notre analyse nous a amené à procéder de la sorte :
Une clause étant une disjonction d'un ensemble de variables propositionelles,
nous en avons donc déduit que
chaque action comprend :
- une clause pour les préconditions
- une clause pour pour chaque effet qu'engendre l'action

On retrouve également une clause pour l'état initial et l'état final.

Pour construire ces clauses, on utilise les fluents.

Lorsque cette étape est faite, on regarde si le IProblem engendré est satisfiable.
S'il l'est, il faut alors décoder le model du solver pour en faire un plan. Pour cela, on a auparavant construit un dictionnaire mappant chacune des actions et fluent
du problem pour pouvoir retrouver le plan par la suite.

## Le résultat final du projet

Nous sommes parvenus au résultat que notre problem est satisfiable mais nous ne sommes pas parvenus à le décoder.
Nous n'avons donc pas pu obtenir de plan, ce qui constituerait la dernière étape du projet.


