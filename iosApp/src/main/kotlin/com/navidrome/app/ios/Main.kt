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
    @Published var currentScreen: Screen = .ArtistList

    func login(baseUrl: String, username: String, password: String, completion: @escaping (Bool) -> Void) {
        Task {
            let result = await authManager.login(baseUrl: baseUrl, username: username, password: password)
            
            await MainActor.run {
                result.fold(
                    onSuccess: { [weak self] in
                        self?.viewModel = SoundtrackViewModel(client: self?.authManager.getClient())
                        self?.isLoggedIn = true
                        self?.viewModel?.loadArtists()
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
                    
                   Url: baseUrl appState.login(base, username: username, password: password) { success in
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
                    .frame(maxWidth: .infinity)
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
            case .ArtistList:
                ArtistListView(viewModel: viewModel)
            case .ArtistDetail:
                ArtistDetailView(viewModel: viewModel)
            case .AlbumDetail:
                AlbumDetailView(viewModel: viewModel)
            case .Player:
                PlayerView(viewModel: viewModel)
            default:
                ArtistListView(viewModel: viewModel)
            }
        }
    }
}

struct ArtistListView: View {
    @ObservedObject var viewModel: SoundtrackViewModel

    var body: some View {
        NavigationStack {
            List(viewModel.artists) { artist in
                Button(action: {
                    viewModel.loadArtistAlbums(artistId: artist.id)
                }) {
                    VStack(alignment: .leading) {
                        Text(artist.name)
                            .font(.headline)
                        Text("\(artist.albumCount) álbumes")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .navigationTitle("Artistas")
            .overlay {
                if viewModel.isLoading {
                    ProgressView()
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
                viewModel.loadAlbumSongs(albumId: album.id)
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
       ButtonHidden .navigationBarBack(true)
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
