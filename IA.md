\# Utilisation de l’IA — TP4 Web



ChatGPT m'a servi a comprendre une erreur. Le cas suivant détaille l’aide concernant l’erreur mémoire.



\## Problème rencontré

L’application affichait une erreur HTTP 500 :

java.lang.OutOfMemoryError: Java heap space.



\## Raison de l’utilisation de l’IA

Comprendre cette erreur et identifier le réglage mémoire de Payara.



\## Prompt utilisé

Peut tu m'expliquer la cause de cette erreur « HTTP Status 500 - Internal Server Error.

jakarta.servlet.ServletException: Java heap space.

Root cause : java.lang.OutOfMemoryError: Java heap space. »



\## Réponse de l’IA

Arrêter Payara, ouvrir le fichier domain.xml avec Bloc-notes,

remplacer la valeur de l’option -Xmx existante par -Xmx2048m,

enregistrer puis redémarrer le serveur.



\## Ce que j’ai appris

L’option -Xmx fixe la taille maximale du tas mémoire Java.

La modification autorise Payara à utiliser jusqu’à 2 Go pour ce tas.

Après le redémarrage, les requêtes RAG ont abouti.

