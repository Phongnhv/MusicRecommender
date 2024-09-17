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

client_id = '64beda4e6b21451283236269ef10aaec'
client_secret = '4f238894f89e43df8b9a5ca2cf977a30'
auth_manager = SpotifyClientCredentials(client_id=client_id, client_secret=client_secret)
sp = spotipy.Spotify(auth_manager=auth_manager)

songID = ""
def find_song(name, nlimit):
    songs_data = []

    result = sp.search(q=name, type='track', limit=1)
    tracks = result['tracks']['items'][0]

    songID = tracks['id']
    artistsID = []

    for artist in  tracks['artists']:
        artistsID.append(artist['id'])

    for artist_id in artistsID:
        results = sp.artist_top_tracks(artist_id)
        print("1!")

        if not results['tracks']:
            return None

        count = 0
        for track in results['tracks']:
            song_data = defaultdict(str)
            track_id = track['id']
            if track_id == songID: continue

            song_data['year'] = int(track['album']['release_date'][:4])
            song_data['name'] = track['name']
            artists = ", ".join(artist['name'] for artist in track['artists'])
            song_data['artist'] = artists
            song_data['cover_url'] = track['album']['images'][0]['url']
            song_data['external_url'] = track['external_urls']['spotify']

            print(song_data['year'])
            print(song_data['name'])
            print(song_data['artist'])
            print(song_data['cover_url'])
            print(song_data['external_url'])

            songs_data.append(song_data)
            count = count + 1
            if count == 4:
                break
    print("1!")
    print("2!")

    for artist_id in artistsID:
        item = sp.artist(artist_id)
        #print("1!")

        artist_name = item['name']
        print(artist_name)
        cover_url = item['images'][0]['url'] if item['images'] else None
        if  cover_url == None: print("None")
        else : print(cover_url)
        print(item['id'])
        spotify_url = item['external_urls']['spotify']
        if  spotify_url == None: print("None")
        else : print(spotify_url)
        follower = item['followers']['total']
        print(follower)

    print("2!")
    count = 0
    related_artists = sp.artist_related_artists(artistsID[0])
    for item in related_artists['artists']:
        if item['id'] in artistsID: continue
        artist_name = item['name']
        print(artist_name)
        cover_url = item['images'][0]['url'] if item['images'] else None
        if  cover_url == None: print("None")
        else : print(cover_url)
        print(item['id'])
        spotify_url = item['external_urls']['spotify']
        if  spotify_url == None: print("None")
        else : print(spotify_url)
        follower = item['followers']['total']
        print(follower)
        count = count + 1
        if count == 4: break

    return songs_data

def main():
    song_name = sys.argv[1]
    #limit = sys.argv[2]
    #print (song_name)

    song_data = find_song(song_name, 5)
    #file_name = "song_data.json"


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


if __name__ == "__main__":
    main()


