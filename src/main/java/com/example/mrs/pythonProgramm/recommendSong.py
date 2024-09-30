import ast
from collections import defaultdict
import json
import os
import numpy as np
import pandas as pd
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from scipy.spatial.distance import cdist
from joblib import dump, load
import warnings
warnings.filterwarnings("ignore")
import sys

import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

client_id = 'CLIENT_ID'
client_secret = 'CLIENT_SECRET'
auth_manager = SpotifyClientCredentials(client_id=client_id, client_secret=client_secret)
sp = spotipy.Spotify(auth_manager=auth_manager)

def find_song(name, year):
    song_data = defaultdict()
    results = sp.search(q=name, type='track', limit=1)
    if results['tracks']['items'] == []:
        return None

    results = results['tracks']['items'][0]
    track_id = results['id']
    audio_features = sp.audio_features(track_id)[0]

    song_data['name'] = results['name']
    song_data['year'] = [year]
    song_data['explicit'] = [int(results['explicit'])]
    song_data['duration_ms'] = [results['duration_ms']]
    song_data['popularity'] = [results['popularity']]

    for key, value in audio_features.items():
        song_data[key] = value

    return pd.DataFrame(song_data)

features_cols = ['valence', 'year', 'acousticness', 'danceability', 'duration_ms', 'energy', 'explicit',
                 'instrumentalness', 'key', 'liveness', 'loudness', 'mode', 'popularity', 'speechiness', 'tempo']

def add_song_clusters(song_list, spotify_data ,centroids, cluster):
    vectors = cluster

    for song in song_list:
        matching_songs = get_song_in_df(song,spotify_data)
        if matching_songs == 0:
            song_data = find_song(song['name'],song['year'])
            vector_df = pd.DataFrame(song_data[features_cols])
            scaler = StandardScaler()
            scaled_data = scaler.fit_transform(spotify_data[features_cols])
            scaled_vector = scaler.transform(vector_df[features_cols])
            distances = cdist(scaled_vector, centroids, 'cosine')
            index = list(np.argsort(distances)[:, :1][0])
            #print(index[0])
            vectors = np.append(vectors,index[0])

        else:
            continue
    return vectors

def get_song_in_df(song, spotify_data):
    try:
        song_data = spotify_data[(spotify_data['name'] == song['name']) & (spotify_data['year'] == song['year'])].iloc[0]
        return 1

    except IndexError:
        return 0

def get_song_data(song, spotify_data):

    #try:
    #    song_data = spotify_data[(spotify_data['name'] == song['name']) & (spotify_data['year'] == song['year'])].iloc[0]
    #    return song_data

    #except IndexError:
    return find_song(song['name'], song['year'])

def get_mean_features_vector(song_list, spotify_data):

    vectors = []

    for song in song_list:
        song_data = get_song_data(song, spotify_data)
        if song_data is None:
            print('Warning: {} does not exist in database'.format(song['name']))
            continue
        vector = song_data[features_cols].values
        vectors.append(vector)

    song_matrix = np.array(list(vectors))
    return np.mean(song_matrix, axis=0)

def get_cluster(song_list, spotify_data):
    song_list_df = pd.DataFrame(song_list, columns=['name', 'year'])
    merged_df = pd.merge(song_list_df, spotify_data[['name', 'year', 'cluster']], on=['name', 'year'])

    return merged_df['cluster'].unique()

def flatten_dict_list(dict_list):

    flattened_dict = defaultdict()
    for key in dict_list[0].keys():
        flattened_dict[key] = []

    for dictionary in dict_list:
        for key, value in dictionary.items():
            flattened_dict[key].append(value)

    return flattened_dict

def recommend_songs(song_list, spotify_data, n_songs=10):

    prime_cols = ['name', 'year', 'artists']
    song_dict = flatten_dict_list(song_list)
    unique_cluster = get_cluster(song_list, spotify_data)
    unique_cluster = add_song_clusters(song_list, spotify_data, centroids, unique_cluster)
    #print(unique_cluster)

    song_center = get_mean_features_vector(song_list, spotify_data)
    scaler = StandardScaler()
    spotify_data_1 = spotify_data[spotify_data['cluster'].isin(unique_cluster)]
    scaled_data = scaler.fit_transform(spotify_data[features_cols])
    scaled_song_center = scaler.transform(song_center.reshape(1, -1))
    distances = cdist(scaled_song_center, scaled_data, 'cosine')
    index = list(np.argsort(distances)[:, :n_songs][0])

    rec_songs = spotify_data.iloc[index]
    rec_songs = rec_songs[~rec_songs['name'].isin(song_dict['name'])]
    ##rec_songs = rec_songs[rec_songs['cluster'].isin(unique_cluster)]
    return rec_songs[prime_cols].to_dict(orient='records')

data = load('data.joblib')
data = data.drop_duplicates(subset='name', keep=False)
centroids = load('song_centroids.joblib')

def find_song_data(name):
    songs_data = []
    results = sp.search(q=name, type='track', limit=1)
    if results['tracks']['items'] == []:
        return None

    if results['tracks']['items'] == []:
        return None

    for track in results['tracks']['items']:
        song_data = defaultdict()
        track_id = track['id']

        song_data['year'] = str(track['album']['release_date'][:4])
        song_data['name'] = track['name']
        art = ""
        for artist in track['artists']:
            art = art + artist['name'] + ", "
        art = art[:-2]
        song_data['artist'] = art
        song_data['cover_url'] = track['album']['images'][0]['url']
        d = track['external_urls']
        song_data['external_url'] = d['spotify']

        songs_data.append(song_data)

    return songs_data

def main():
    input_str = sys.argv[1]
    song_list = ast.literal_eval(input_str)

    ##print(song_list)
    ##recommends = recommend_songs([{'name': 'Come As You Are', 'year':1991},
    ##            {'name': 'Smells Like Teen Spirit', 'year': 1991}],  data)

    ##print(recommends)
    ##print(find_song('Still here with you', 2024))
    ##cluster = []
    ##add_song_clusters([{'name': 'Still here with you', 'year':2024}], data, centroids,cluster)
    ##print(find_song('Still here with you', 2024))
    recommends = recommend_songs(song_list,data)
    #recommends.replace({r'[^\x00-\x7F]+':''}, regex=True, inplace=True)
    rec_vectors = []
    for song_metadata in recommends:
        vector = find_song_data(song_metadata['name'] + song_metadata['artists'])
        print(vector[0]['year'])
        print(vector[0]["name"])
        print(vector[0]["artist"])
        print(vector[0]["cover_url"])
        print(vector[0]["external_url"])
        rec_vectors.append(vector)
    ##print(recommends)
    ##clus = get_cluster(recommends, data)
    ##print(clus)


if __name__ == "__main__":
    main()
