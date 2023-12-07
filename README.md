# DosOok - Transmission de Données par le Son

Le projet DosOok vise à concevoir un ensemble de deux programmes Java permettant l'échange de données numériques via un canal audio. L'utilisation de la technologie Data over Sound offre une alternative sans matériel spécialisé, exploitant les haut-parleurs et microphones disponibles sur n'importe quel appareil.

## Pourquoi DosOok?
- Flexibilité: Transmettez des données entre n'importe quel appareil doté d'un haut-parleur et/ou d'un microphone.
- Sécurité: La cryptographie garantit un transfert rapide et sécurisé de données.
- Frictionless: Contribue à une expérience utilisateur sans friction.

## Les Signaux Audio Numériques
Les signaux numériques sont des suites de valeurs définies à des instants d'échantillonnage, représentées selon un format donné (par exemple, 8 bits ou 16 bits). La technologie utilise une fréquence d'échantillonnage de 44,1 kHz, liée à la bande passante maximale de l'oreille humaine.

## Analog vs. Numérique
Transmission sans Fil des Signaux Audio
La modulation d'amplitude par sauts (ASK) est utilisée pour transmettre les signaux audio. La porteuse, une sinusoïde de 1 kHz, est modulée par la traduction binaire des données à transmettre. La transmission est efficace sur de longues distances, assurant la robustesse du signal.

## ASK
Mémorisation des Données Audio: Format RIFF (WAV)
Les signaux audio sont stockés dans des fichiers WAV pour faciliter la mise au point. Les paramètres imposés incluent une fréquence d'échantillonnage de 44,1 kHz, un format PCM entier, un seul canal (mono), et 16 bits par échantillon.

## DosSend - Programme Émetteur
Le programme DosSend génère des signaux audio modulés en ASK à partir de fichiers texte. L'exécution produit des informations sur le message, le nombre de symboles, d'échantillons, et la durée.

# Exemple:

Input :
```bash
java DosSend < ./helloWorld.txt
```

Output :
```
Message : Hello World !
    Nombre de symboles : 13
    Nombre d'échantillons : 49392
    Durée : 1.12 s
```

## DosRead - Programme Récepteur
Le programme DosRead décode les signaux audio enregistrés, extrayant les données transmises. Il lit l'entête du fichier WAV, les données audio, et applique des filtres pour extraire le message. Enfin, il décode et affiche le message.

Exemple:

Input : 
```bash
java DosRead ./message_DosOok.wav
```

Output:
```
Message décodé : H e l l o   W o r l d   !
```
