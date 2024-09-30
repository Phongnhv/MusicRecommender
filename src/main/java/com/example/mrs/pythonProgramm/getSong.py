import ast
from collections import defaultdict
import json
import os
import numpy as np
import pandas as pd
import warnings
import joblib
warnings.filterwarnings("ignore")
import sys
import mysql.connector

import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

client_id = 'CLIENT_ID'
client_secret = 'CLIENT_SECRET'
auth_manager = SpotifyClientCredentials(client_id=client_id, client_secret=client_secret)
sp = spotipy.Spotify(auth_manager=auth_manager)

def find_song(name, nlimit):
    songs_data = []
    results = sp.search(q=name, type='track', limit=nlimit)
    if results['tracks']['items'] == []:
        return None

    if results['tracks']['items'] == []:
        return None

    for track in results['tracks']['items']:
        song_data = defaultdict()
        track_id = track['id']

        song_data['year'] = int(track['album']['release_date'][:4])
        song_data['name'] = track['name']
        artists = ""
        for artist in track['artists']:
            artists = artists + artist['name'] + ", "
        artists = artists[:-2]
        song_data['artist'] = artists
        song_data['cover_url'] = track['album']['images'][0]['url']
        d = track['external_urls']
        song_data['external_url'] = d['spotify']

        songs_data.append(song_data)

    return songs_data

connection = mysql.connector.connect(
    host="localhost",
    user="root",
    password="",
    database="mr_sys"
)
cursor = connection.cursor()

def main():
    song_name = sys.argv[1]
    limit = sys.argv[2]
    #print (song_name)

    song_data = find_song(song_name, limit)
    #file_name = "song_data.json"
    for song in song_data:
        print(song['year'])
        print('\n')
        print(song['name'] + '\n')
        print(song['artist'] + '\n')
        print(song['cover_url'] + '\n')
        print(song['external_url'] + '\n')
    #print(song_data)
    #json.dump(song_data, open(file_name, "w"), indent=4)

    ##print(name_list)

    ##for index, row in name_list.iterrows():
    #    song_name = row['name']
    #    song_dat = (find_song(song_name))
    #    if song_dat == None:
    #        continue
    #   artist = song_dat[0]['artist']
    #    year = song_dat[0]['year']
    #    cover_url = song_dat[0]['corver_url']
    #    d = song_dat[0]['external_url']
    #    ex_url = d['spotify']
    #    sql = "INSERT INTO local_song_data (name,artist, cover_url, external_url, year) VALUES (%s, %s, %s, %s, %s)"
    #    values = (song_name, artist,cover_url,ex_url,year)
    #    cursor.execute(sql, values)
    #    connection.commit()

    connection.close()


if __name__ == "__main__":
    main()


