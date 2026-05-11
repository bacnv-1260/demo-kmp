import UIKit
import shared

class UserTableViewCell: UITableViewCell {
    static let reuseId = "UserTableViewCell"

    private let avatarImageView = UIImageView()
    private let loginLabel = UILabel()
    private let typeLabel = UILabel()
    private var imageTask: Task<Void, Never>?

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }

    required init?(coder: NSCoder) { fatalError("not implemented") }

    func configure(with user: GitHubUser) {
        loginLabel.text = user.login
        typeLabel.text = user.type
        loadAvatar(urlString: user.avatarUrl)
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        imageTask?.cancel()
        avatarImageView.image = nil
    }

    private func loadAvatar(urlString: String) {
        guard let url = URL(string: urlString) else { return }
        imageTask = Task {
            guard let (data, _) = try? await URLSession.shared.data(from: url),
                  !Task.isCancelled,
                  let image = UIImage(data: data) else { return }
            await MainActor.run { [weak self] in
                self?.avatarImageView.image = image
            }
        }
    }

    private func setupUI() {
        avatarImageView.contentMode = .scaleAspectFill
        avatarImageView.clipsToBounds = true
        avatarImageView.layer.cornerRadius = 22
        avatarImageView.backgroundColor = .secondarySystemFill
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false

        loginLabel.font = .systemFont(ofSize: 16, weight: .semibold)
        loginLabel.translatesAutoresizingMaskIntoConstraints = false

        typeLabel.font = .systemFont(ofSize: 12)
        typeLabel.textColor = .secondaryLabel
        typeLabel.translatesAutoresizingMaskIntoConstraints = false

        let stack = UIStackView(arrangedSubviews: [loginLabel, typeLabel])
        stack.axis = .vertical
        stack.spacing = 2
        stack.translatesAutoresizingMaskIntoConstraints = false

        contentView.addSubview(avatarImageView)
        contentView.addSubview(stack)

        NSLayoutConstraint.activate([
            avatarImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            avatarImageView.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            avatarImageView.widthAnchor.constraint(equalToConstant: 44),
            avatarImageView.heightAnchor.constraint(equalToConstant: 44),

            stack.leadingAnchor.constraint(equalTo: avatarImageView.trailingAnchor, constant: 12),
            stack.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            stack.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
        ])
    }
}
