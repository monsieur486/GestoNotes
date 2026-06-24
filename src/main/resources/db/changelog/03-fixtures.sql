INSERT INTO categories (id, denomination, est_active, est_modifiable)
VALUES
(1,'🏠 Principale',true,false),
(2,'💶 Catégorie 1',true,true),
(3,'💵 Catégorie 2',true,true),
(4,'🌎 Catégorie 3',true,true),
(5,'📫 Catégorie 4',true,true),
(6,'☎︎ Catégorie 5',false,true);

INSERT INTO notes (categorie_id, titre, couleur, contenu)
VALUES
(1, 'Coucou', 1, 'Coucou ma Darling ❤︎'),
(2, 'Test couleur 1', 1, 'Test 1'),
(2, 'Test couleur 2', 2, 'Test 2'),
(2, 'Test couleur 3', 3, 'Test 3'),
(2, 'Test couleur 4', 4, 'Test 4');