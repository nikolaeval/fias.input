# fias.input
The utility for indexing and entering address by the FIAS directory

The project is intended for downloading, indexing and providing API for searching and entering addresses by the FIAS directory
As a search engine, Elasticsearch is used (https://www.elastic.co/)

The fias.input.loader utility loads the FIAS database from the site (https://fias.nalog.ru/Updates.aspx), and also creates a search index.
During the download process the archives are not unpacked, and the data is read directly from it. 
Therefore, there is no need for additional space other than the size of the downloaded files (<6Gb for full DB).
To use morphological search by name, you need to install the "Morphological Analysis Plugin" (https://github.com/imotov/elasticsearch-analysis-morphology).
Otherwise, you need to exclude it from the script to create an index structure (fias.input.lib/src/main/resources/es/CreateIndex.json)

The load parameters are described in the configuration file fias.input.loader/config/application.properties.
The configuration file must be located in the config directory relative to the start path.
Command line example: java -Dlogback.configurationFile=file:./config/logback.xml -jar fias.input.loader.jar

It supports both full and incremental loading. But delta files do not guarantee the data consistency.
It is recommended to perform an update only on a full database, and periodically do a "clean" load.

The download supports a region filter (region.filter parameter).
In this case, the index file http://vinfo.russianpost.ru/database/PIndx.zip is used to compare the postcodes of houses and region codes.
The custom_region_codes parameter can be used to uniquely match the region code with postcode name.

The fias.input.lib project contains the base classes for working with the address index
The fias.input.web is only a test example (web application) demonstrating a possible input component
