import SwiftUI
import AVFoundation

@main
struct SoundtrackApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView(appState: appState)
        }
    }
}

class AppState: ObservableObject {
    let authManager = AuthManager()
    var viewModel: SoundtrackViewModel? = nil
    
    @Published var isLoggedIn: Bool = false

    func login(baseUrl: String, username: String, password: String, completion: @escaping (Bool) -> Void) {
        Task {
            let result = await authManager.login(baseUrl: baseUrl, username: username, password: password)
            
            await MainActor.run {
                result.fold(
                    onSuccess: { [weak self] in
                        self?.viewModel = SoundtrackViewModel(client: self?.authManager.getClient())
                        self?.isLoggedIn = true
                        self?.viewModel?.loadHomeData()
                        completion(true)
                    },
                    onFailure: { _ in
                        completion(false)
                    }
                )
            }
        }
    }
}

struct ContentView: View {
    @ObservedObject var appState: AppState

    var body: some View {
        if appState.isLoggedIn, let vm = appState.viewModel {
            MainView(viewModel: vm)
        } else {
            LoginView(appState: appState)
        }
    }
}

struct LoginView: View {
    @ObservedObject var appState: AppState
    @State private var baseUrl: String = ""
    @State private var username: String = ""
    @State private var password: String = ""
    @State private var isLoading: Bool = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Text("Conectar a Navidrome")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("URL del servidor")
                            .font(.subheadline)
                        TextField("http://tu-servidor:4533", text: $baseUrl)
                            .textFieldStyle(.roundedBorder)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Usuario")
                            .font(.subheadline)
                        TextField("Usuario", text: $username)
                            .textFieldStyle(.roundedBorder)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Contraseña")
                            .font(.subheadline)
                        SecureField("Contraseña", text: $password)
                            .textFieldStyle(.roundedBorder)
                    }
                }
                .padding(.horizontal)
                
                if let error = errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                        .padding()
                        .background(Color.red.opacity(0.1))
                        .cornerRadius(8)
                }
                
                Button(action: {
                    isLoading = true
                    errorMessage = nil
                    
                    appState.login(baseUrl: baseUrl, username: username, password: password) { success in
                        isLoading = false
                        if !success {
                            errorMessage = "Error al conectar"
                        }
                    }
                }) {
                    HStack {
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Text("Conectar")
                        }
                    }
                   : .infinity .frame(maxWidth)
                    .padding()
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(10)
                }
                .disabled(isLoading || baseUrl.isEmpty || username.isEmpty || password.isEmpty)
                .padding(.horizontal)
                
                Spacer()
            }
            .padding(.top, 60)
        }
    }
}

struct MainView: View {
    @ObservedObject var viewModel: SoundtrackViewModel

    var body: some View {
        Group {
            switch viewModel.currentScreen {
            case .Home:
                HomeView(viewModel: viewModel)
            case .Search:
                SearchView(viewModel: viewModel)
            case .ArtistDetail:
                ArtistDetailView(viewModel: viewModel)
            case .AlbumDetail:
                AlbumDetailView(viewModel: viewModel)
            case .Player:
                PlayerView(viewModel: viewModel)
            default:
                HomeView(viewModel: viewModel)
            }
        }
    }
}

struct HomeView: View {
    @ObservedObject var viewModel: SoundtrackViewModel
    @State private var searchText: String = ""

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField("Buscar...", text: $searchText)
                        .textFieldStyle(.plain)
                        .onSubmit {
                            if !searchText.isEmpty {
                                viewModel.search(query: searchText)
                            }
                        }
                    if !searchText.isEmpty {
                        Button(action: { searchText = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding()
                
                if viewModel.isLoading {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 16) {
                            // Artists Section
                            SectionView(title: "Artistas") {
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 12) {
                                        ForEach(viewModel.artists) { artist in
                                            ArtistCard(artist: artist) {
                                                viewModel.loadArtistAlbums(artistId: artist.id)
                                            }
                                        }
                                    }
                                    .padding(.horizontal)
                                }
                            }
                            
                            // Albums Section
                            SectionView(title: "Álbumes") {
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 12) {
                                        ForEach(viewModel.allAlbums) { album in
                                            AlbumCard(album: album) {
                                                viewModel.openAlbum(albumId: album.id)
                                            }
                                        }
                                    }
                                    .padding(.horizontal)
                                }
                            }
                            
                            // Songs Section
                            SectionView(title: "Canciones") {
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 12) {
                                        ForEach(viewModel.allSongs) { song in
                                            SongCard(song: song) {
                                                viewModel.playSong(song: song, playlist: viewModel.allSongs)
                                            }
                                        }
                                    }
                                    .padding(.horizontal)
                                }
                            }
                        }
                        .padding(.bottom)
                    }
                }
            }
            .navigationTitle("Soundtrack")
        }
    }
}

struct SectionView<Content: View>: View {
    let title: String
    @ViewBuilder let content: () -> Content
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.title2)
                .fontWeight(.bold)
                .padding(.horizontal)
            content()
        }
    }
}

struct ArtistCard: View {
    let artist: Artist
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack {
                Image(systemName: "person.circle.fill")
                    .font(.system(size: 50))
                    .foregroundColor(.blue)
                Text(artist.name)
                    .font(.caption)
                    .lineLimit(2)
                    .multilineTextAlignment(.center)
                    .frame(width: 80)
            }
            .frame(width: 80)
        }
        .buttonStyle(.plain)
    }
}

struct AlbumCard: View {
    let album: Album
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading) {
                Image(systemName: "square.stack")
                    .font(.system(size: 50))
                    .foregroundColor(.orange)
                    .frame(width: 100, height: 80)
                Text(album.name)
                    .font(.caption)
                    .lineLimit(1)
                    .frame(width: 100, alignment: .leading)
                Text(album.artist)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            .frame(width: 100)
        }
        .buttonStyle(.plain)
    }
}

struct SongCard: View {
    let song: Song
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading) {
                Text(song.title)
                    .font(.subheadline)
                    .lineLimit(1)
                Text(song.artist)
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .lineLimit(1)
            }
            .frame(width: 150, alignment: .leading)
            .padding(12)
            .background(Color(.systemGray6))
            .cornerRadius(8)
        }
        .buttonStyle(.plain)
    }
}

struct SearchView: View {
    @ObservedObject var viewModel: SoundtrackViewModel
    
    var body: some View {
        List {
            if !viewModel.searchResults.artists.isEmpty {
                Section("Artistas") {
                    ForEach(viewModel.searchResults.artists) { artist in
                        Button(action: {
                            viewModel.loadArtistAlbums(artistId: artist.id)
                        }) {
                            Text(artist.name)
                        }
                    }
                }
            }
            
            if !viewModel.searchResults.albums.isEmpty {
                Section("Álbumes") {
                    ForEach(viewModel.searchResults.albums) { album in
                        Button(action: {
                            viewModel.openAlbum(albumId: album.id)
                        }) {
                            VStack(alignment: .leading) {
                                Text(album.name)
                                Text(album.artist)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
            }
            
            if !viewModel.searchResults.songs.isEmpty {
                Section("Canciones") {
                    ForEach(viewModel.searchResults.songs) { song in
                        Button(action: {
                            viewModel.playSong(song: song, playlist: viewModel.searchResults.songs)
                        }) {
                            VStack(alignment: .leading) {
                                Text(song.title)
                                Text(song.artist)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle("Resultados")
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Volver") {
                    viewModel.clearSearch()
                    viewModel.goBack()
                }
            }
        }
    }
}

struct ArtistDetailView: View {
    @ObservedObject var viewModel: SoundtrackViewModel

    var body: some View {
        List(viewModel.albums) { album in
            Button(action: {
                viewModel.openAlbum(albumId: album.id)
            }) {
                VStack(alignment: .leading) {
                    Text(album.name)
                        .font(.headline)
                    Text("\(album.songCount) canciones")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
        }
        .navigationTitle(viewModel.currentArtist?.name ?? "Artista")
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Volver") {
                    viewModel.goBack()
                }
            }
        }
    }
}

struct AlbumDetailView: View {
    @ObservedObject var viewModel: SoundtrackViewModel

    var body: some View {
        List(Array(viewModel.songs.enumerated()), id: \.element.id) { index, song in
            Button(action: {
                viewModel.playSong(song: song, playlist: viewModel.songs)
            }) {
                HStack {
                    Text("\(song.track ?? index + 1)")
                        .foregroundColor(.secondary)
                        .frame(width: 30)
                    VStack(alignment: .leading) {
                        Text(song.title)
                            .font(.headline)
                        Text(formatDuration(song.duration))
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .navigationTitle(viewModel.currentAlbum?.name ?? "Álbum")
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Volver") {
                    viewModel.goBack()
                }
            }
        }
    }
}

struct PlayerView: View {
    @ObservedObject var viewModel: SoundtrackViewModel

    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            
            Image(systemName: "music.note")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 120, height: 120)
                .foregroundColor(.blue)
            
            VStack(spacing: 8) {
                Text(viewModel.currentSong?.title ?? "Sin canción")
                    .font(.title2)
                    .fontWeight(.bold)
                
                Text(viewModel.currentSong?.artist ?? "")
                    .font(.body)
                    .foregroundColor(.secondary)
                
                Text(viewModel.currentSong?.album ?? "")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            HStack(spacing: 40) {
                Button(action: {
                    viewModel.previousSong()
                }) {
                    Image(systemName: "backward.fill")
                        .font(.title)
                }
                
                Button(action: {
                    viewModel.togglePlayPause()
                }) {
                    Image(systemName: viewModel.isPlaying ? "pause.circle.fill" : "play.circle.fill")
                        .font(.largeTitle)
                }
                .buttonStyle(.plain)
                
                Button(action: {
                    viewModel.nextSong()
                }) {
                    Image(systemName: "forward.fill")
                        .font(.title)
                }
            }
            
            Spacer()
        }
        .padding()
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button("Volver") {
                    viewModel.goBack()
                }
            }
        }
    }
}

func formatDuration(_ seconds: Int) -> String {
    let mins = seconds / 60
    let secs = seconds % 60
    return String(format: "%d:%02d", mins, secs)
}
