import SwiftUI

@main
struct NavidromeApp: App {
    @StateObject private var authManager = AuthManagerObservable()

    var body: some Scene {
        WindowGroup {
            ContentView(authManager: authManager)
        }
    }
}

class AuthManagerObservable: ObservableObject {
    let authManager = AuthManager()
    
    @Published var isLoggedIn: Bool = false
    @Published var baseUrl: String = "http://music.elgurudekatmandu.com"
    @Published var username: String = "openclaw"
    @Published var password: String = "bootstrap"
    @Published var isLoading: Bool = false
    @Published var errorMessage: String? = nil
    
    func login() {
        isLoading = true
        errorMessage = nil
        
        Task {
            let result = await authManager.login(baseUrl: baseUrl, username: username, password: password)
            
            await MainActor.run {
                result.fold(
                    onSuccess: { [weak self] in
                        self?.isLoggedIn = true
                    },
                    onFailure: { [weak self] e in
                        self?.errorMessage = "Error al conectar: \(e.localizedDescription)"
                    }
                )
                self.isLoading = false
            }
        }
    }
}

struct ContentView: View {
    @ObservedObject var authManager: AuthManagerObservable

    var body: some View {
        if authManager.isLoggedIn {
            LoggedInView(authManager: authManager)
        } else {
            LoginView(authManager: authManager)
        }
    }
}

struct LoginView: View {
    @ObservedObject var authManager: AuthManagerObservable

    var body: some View {
        VStack(spacing: 24) {
            Text("Conectar a Navidrome")
                .font(.largeTitle)
                .fontWeight(.bold)
            
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("URL del servidor")
                        .font(.subheadline)
                    TextField("http://tu-servidor:4533", text: $authManager.baseUrl)
                        .textFieldStyle(.roundedBorder)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Usuario")
                        .font(.subheadline)
                    TextField("Usuario", text: $authManager.username)
                        .textFieldStyle(.roundedBorder)
                        .autocapitalization(.none)
                        .disableAutocorrection(true)
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Contraseña")
                        .font(.subheadline)
                    SecureField("Contraseña", text: $authManager.password)
                        .textFieldStyle(.roundedBorder)
                }
            }
            .padding(.horizontal)
            
            if let error = authManager.errorMessage {
                Text(error)
                    .foregroundColor(.red)
                    .font(.caption)
                    .padding()
                    .background(Color.red.opacity(0.1))
                    .cornerRadius(8)
            }
            
            Button(action: {
                authManager.login()
            }) {
                HStack {
                    if authManager.isLoading {
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
            .disabled(authManager.isLoading || authManager.baseUrl.isEmpty || authManager.username.isEmpty || authManager.password.isEmpty)
            .padding(.horizontal)
            
            Spacer()
        }
        .padding(.top, 60)
    }
}

struct LoggedInView: View {
    @ObservedObject var authManager: AuthManagerObservable

    var body: some View {
        VStack(spacing: 16) {
            Text("¡Conectado a Navidrome!")
                .font(.title)
            
            Text("Usuario: \(authManager.authManager.getUsername())")
            Text("URL: \(authManager.authManager.getBaseUrl())")
            
            Spacer()
        }
        .padding()
    }
}
